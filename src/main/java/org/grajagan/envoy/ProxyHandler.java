package org.grajagan.envoy;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.SocketTimeoutException;
import java.net.URI;
import java.net.URL;
import java.net.URLDecoder;
import java.util.List;
import java.util.Map.Entry;
import java.util.zip.DataFormatException;

import javax.net.ssl.HttpsURLConnection;

import org.apache.commons.io.IOUtils;
import org.apache.log4j.Logger;
import org.grajagan.http.HttpHeaders;
import org.grajagan.ssl.HttpsServerFactory;
import org.grajagan.ssl.HttpsURLConnectionFactory;
import org.grajagan.zlib.CompressionUtils;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpsServer;

public class ProxyHandler implements HttpHandler {

    public static final int TIMEOUT = 20000;
    private final URL remoteUrl;
    private File temporaryDirectory;
    private static final String prefix = "proxy-";

    private static final Logger LOG = Logger.getLogger(ProxyHandler.class);

    public ProxyHandler(URL upstreamUrl) {
        this.remoteUrl = upstreamUrl;
        temporaryDirectory = new File("/var/spool/envoy");
        if (!temporaryDirectory.exists()) {
            if (!temporaryDirectory.mkdirs()) {
                LOG.warn("Cannot log to " + temporaryDirectory
                        + ", resorting to default temporary directory");
                temporaryDirectory = null;
            }
        }
    }

    @Override
    public void handle(HttpExchange clientExchange) throws IOException {
        LOG.debug("Received request for " + clientExchange.getRequestURI());

        HttpsURLConnection remoteConnection = getRemoteConnection(clientExchange);

        // copy client request headers to remote request
        HttpHeaders requestHeaders = new HttpHeaders(clientExchange.getRequestHeaders());
        for (Entry<String, List<String>> entry : requestHeaders.entrySet()) {
            String key = entry.getKey();
            for (String value : entry.getValue()) {
                remoteConnection.setRequestProperty(key, value);
            }
        }

        // copy client request to array in order to save even if connection times out
        InputStream is = clientExchange.getRequestBody();
        byte[] requestBytes = ccopy(is, null);

        File requestFile = File.createTempFile(prefix, ".request", temporaryDirectory);
        String requestString =
                pickle(requestFile, clientExchange.getRequestMethod() + " "
                        + remoteConnection.getURL().toString(), requestHeaders, requestBytes);

        saveXML(requestString);

        OutputStream os = null;
        try {
            os = remoteConnection.getOutputStream();
        } catch (SocketTimeoutException e) {
            LOG.warn("timeout while trying to access remote host");
            clientExchange.close();
            return;
        }

        // now send the client request to the remote
        IOUtils.write(requestBytes, os);
        os.close();

        // copy remote response headers to client response
        HttpHeaders responseHeaders = new HttpHeaders(remoteConnection.getHeaderFields());
        clientExchange.getResponseHeaders().putAll(responseHeaders);

        // send the headers to the client
        clientExchange.sendResponseHeaders(remoteConnection.getResponseCode(), responseHeaders
                .getContentLength());

        // copy remote response to client
        is = remoteConnection.getInputStream();
        os = clientExchange.getResponseBody();
        byte[] responseBytes = ccopy(is, os);

        File responseFile = File.createTempFile(prefix, ".response", temporaryDirectory);
        pickle(responseFile, responseHeaders.getFirst(null), responseHeaders, responseBytes);

        os.close();
        clientExchange.close();
    }

    private String pickle(File file, String method, HttpHeaders headers, byte[] body)
            throws IOException {
        FileOutputStream fos = new FileOutputStream(file);

        LOG.debug("writing to " + file);
        IOUtils.write(method + "\n\n", fos);
        IOUtils.write(headers.toString() + "\n", fos);

        byte[] bytes = body;
        if (headers.isDeflated()) {
            try {
                bytes = CompressionUtils.decompress(body);
            } catch (DataFormatException e) {
                LOG.warn("Cannot decompress data", e);
            }
        }

        String string = new String(bytes);
        IOUtils.write(string, fos);
        fos.close();
        return string;
    }

    private static byte[] ccopy(InputStream input, OutputStream output) throws IOException {
        int n = 0;
        byte[] buffer = new byte[4096];
        ByteArrayOutputStream ccos = new ByteArrayOutputStream();
        while (-1 != (n = input.read(buffer))) {
            if (output != null) {
                output.write(buffer, 0, n);
            }
            ccos.write(buffer, 0, n);
        }
        LOG.debug("CCopied " + ccos.size() + " bytes ");
        return ccos.toByteArray();
    }

    protected HttpsURLConnection getRemoteConnection(HttpExchange clientExchange)
            throws IOException {
        HttpsURLConnection remoteConnection = null;
        URL url =
                new URL(remoteUrl.getProtocol(), remoteUrl.getHost(), remoteUrl.getPort(),
                        clientExchange.getRequestURI().toString());

        try {
            remoteConnection = HttpsURLConnectionFactory.createHttpsURLConnection(url);
        } catch (Exception e) {
            LOG.error("Cannot create remote connection", e);
            throw new IOException(e);
        }

        remoteConnection.setRequestMethod(clientExchange.getRequestMethod());
        remoteConnection.setDoInput(true);
        remoteConnection.setDoOutput(true);
        remoteConnection.setConnectTimeout(TIMEOUT);
        return remoteConnection;
    }

    private void saveXML(String requestString) throws IOException {
        File xml = File.createTempFile(prefix, ".xml", temporaryDirectory);
        for (String keyValue : requestString.split("&")) {
            String[] pair = keyValue.split("=");
            if (pair[0].equals("body")) {
                LOG.debug("writing xml to " + xml);
                FileOutputStream fos = new FileOutputStream(xml);
                IOUtils.write(URLDecoder.decode(pair[1], "UTF-8"), fos);
                fos.close();
                return;
            }
        }
        LOG.warn("no body key found");
        xml.delete();
    }

    public File getTemporaryDirectory() {
        return temporaryDirectory;
    }

    public void setTemporaryDirectory(File temporaryDirectory) {
        this.temporaryDirectory = temporaryDirectory;
    }

    /**
     * Main method to debug connections to the server on a given port.
     * 
     * @param argv
     *            the port to connect to as a number
     * @throws Exception
     *             if any errors occur
     */
    public static void main(String[] argv) throws Exception {
        final int defaultPort = 7777;
        int port = defaultPort;
        String remote = "https://mail.grajagan.org";
        String host = "localhost";

        if (argv.length > 0) {
            remote = argv[0];
        }

        if (argv.length > 1) {
            host = argv[1];
        }

        if (argv.length > 2) {
            port = Integer.parseInt(argv[2]);
        }

        LOG.debug("Starting proxy for " + remote + " on " + host + " and port " + port);
        HttpsServer server = HttpsServerFactory.createServer(host, port);
        URL url = URI.create(remote).toURL();
        server.createContext("/", new ProxyHandler(url));
        server.start();
        while (true) {
        }
    }
}
