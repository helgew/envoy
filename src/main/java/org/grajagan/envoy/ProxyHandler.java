package org.grajagan.envoy;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.net.URL;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.util.List;
import java.util.Map.Entry;
import java.util.zip.DataFormatException;
import java.util.zip.DeflaterOutputStream;
import java.util.zip.InflaterInputStream;

import javax.net.ssl.HttpsURLConnection;

import org.apache.commons.io.IOUtils;
import org.apache.log4j.Logger;
import org.grajagan.CompressionUtils;
import org.grajagan.ssl.HttpsServerFactory;
import org.grajagan.ssl.HttpsURLConnectionFactory;

import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpsServer;

public class ProxyHandler implements HttpHandler {

    private final URL remoteUrl;
    private static final Logger LOG = Logger.getLogger(ProxyHandler.class);

    public ProxyHandler(URL upstreamUrl) {
        this.remoteUrl = upstreamUrl;
    }

    @Override
    public void handle(HttpExchange httpExchange) throws IOException {
        LOG.debug("Received request for " + httpExchange.getRequestURI());

        String prefix = "proxy-";
        File requestFile = File.createTempFile(prefix, ".request");
        File responseFile = File.createTempFile(prefix, ".response");

        URL url =
                new URL(remoteUrl.getProtocol(), remoteUrl.getHost(), remoteUrl.getPort(),
                        httpExchange.getRequestURI().toString());

        HttpsURLConnection remoteConnection = null;
        try {
            remoteConnection = HttpsURLConnectionFactory.createHttpsURLConnection(url);
        } catch (Exception e) {
            LOG.error("Cannot create remote connection", e);
            throw new IOException(e);
        }

        remoteConnection.setRequestMethod(httpExchange.getRequestMethod());
        remoteConnection.setDoInput(true);
        remoteConnection.setDoOutput(true);

        FileOutputStream fos = new FileOutputStream(requestFile);

        Headers headers = httpExchange.getRequestHeaders();

        boolean isDeflated = false;

        StringBuffer hsb = new StringBuffer();
        for (Entry<String, List<String>> entry : headers.entrySet()) {
            String key = entry.getKey();
            for (String value : entry.getValue()) {
                hsb.append(key + ": " + value + "\n");
                remoteConnection.setRequestProperty(key, value);
                if (key.equalsIgnoreCase("content-type")
                        && value.equalsIgnoreCase("application/x-deflate")) {
                    isDeflated = true;
                }
            }
        }

        hsb.append("\n");

        LOG.debug("writing request to " + requestFile);
        IOUtils.write(httpExchange.getRequestMethod() + " " + remoteConnection.getURL().toString()
                + "\n\n", fos);
        IOUtils.write(hsb.toString(), fos);

        InputStream is = httpExchange.getRequestBody();
        String requestString = IOUtils.toString(is);
        IOUtils.write(requestString, fos);
        if (isDeflated) {
            IOUtils.write("\n\nUncompressed:\n", fos);
            try {
                IOUtils.write(CompressionUtils.decompress(requestString.getBytes()), fos);
            } catch (DataFormatException e) {
                LOG.error("cannot decompress data", e);
            }
        }
        fos.close();

        OutputStream os = remoteConnection.getOutputStream();
        if (isDeflated) {
            os = new DeflaterOutputStream(os);
        }
        IOUtils.write(URLEncoder.encode(requestString, "UTF-8"), os);
        os.close();

        int statusCode = remoteConnection.getResponseCode();

        headers = httpExchange.getResponseHeaders();
        String responseMessage = null;
        hsb = new StringBuffer();
        isDeflated = false;
        for (Entry<String, List<String>> entry : remoteConnection.getHeaderFields().entrySet()) {
            String key = entry.getKey();
            for (String value : entry.getValue()) {
                if (key != null) {
                    hsb.append(key + ": " + value + "\n");
                    headers.add(key, value);
                    if (key.equalsIgnoreCase("content-type")
                            && value.equalsIgnoreCase("application/x-deflate")) {
                        isDeflated = true;
                    }
                } else {
                    responseMessage = value;
                }
            }
        }

        hsb.append("\n");
        LOG.debug("writing response to " + responseFile);
        fos = new FileOutputStream(responseFile);
        IOUtils.write(responseMessage + "\n" + hsb.toString(), fos);

        is = remoteConnection.getInputStream();
        if (isDeflated) {
            is = new InflaterInputStream(is);
        }

        String responseString = IOUtils.toString(is);
        IOUtils.write(responseString, fos);
        fos.close();

        httpExchange.sendResponseHeaders(statusCode, responseString.length());
        os = httpExchange.getResponseBody();
        if (isDeflated) {
            os = new DeflaterOutputStream(os);
        }
        IOUtils.write(responseString, os);

        httpExchange.close();
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
        URL url = URI.create(argv[0]).toURL();
        server.createContext("/", new ProxyHandler(url));
        server.start();
        while (true) {
        }
    }

}
