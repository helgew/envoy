package org.grajagan.envoy;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URI;
import java.net.URL;
import java.util.List;
import java.util.Map.Entry;

import javax.net.ssl.HttpsURLConnection;

import org.apache.commons.io.IOUtils;
import org.apache.log4j.Logger;
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
        
        FileOutputStream os = new FileOutputStream(requestFile);
        
        Headers headers = httpExchange.getRequestHeaders();
        
        StringBuffer hsb = new StringBuffer();
        for (Entry<String, List<String>> entry : headers.entrySet()) {
            String key = entry.getKey();
            for (String value : entry.getValue()) {
                hsb.append(key + ": " + value + "\n");
                remoteConnection.setRequestProperty(key, value);
            }
        }

        hsb.append("\n");

        LOG.debug("writing request to " + requestFile);
        IOUtils.write(hsb.toString(), os);

        String requestString = IOUtils.toString(httpExchange.getRequestBody());
        IOUtils.write(requestString, os);
      
        os.close();
        
        remoteConnection.setDoInput(true);
        remoteConnection.setDoOutput(true);
        remoteConnection.connect();
        
        IOUtils.write(requestString, remoteConnection.getOutputStream());
        remoteConnection.getOutputStream().close();
        
        int statusCode = remoteConnection.getResponseCode();
                
        headers = httpExchange.getResponseHeaders();
        String responseMessage = null;
        hsb = new StringBuffer();
        for (Entry<String, List<String>> entry : remoteConnection.getHeaderFields().entrySet()) {
            String key = entry.getKey();
            for (String value : entry.getValue()) {
                if (key != null) {
                    hsb.append(key + ": " + value + "\n");
                    headers.add(key, value);
                } else {
                    responseMessage = value;
                }
            }
        }

        hsb.append("\n");
        LOG.debug("writing response to " + responseFile);
        os = new FileOutputStream(responseFile);
        IOUtils.write(responseMessage + "\n" + hsb.toString(), os);
        
        String responseString = IOUtils.toString(remoteConnection.getInputStream());
        IOUtils.write(responseString, os);
        os.close();
        
        httpExchange.sendResponseHeaders(statusCode, responseString.length());
        IOUtils.write(responseString, httpExchange.getResponseBody());
        
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
        if (argv.length > 0) {
            port = Integer.parseInt(argv[0]);
        }
        HttpsServer server = HttpsServerFactory.createServer("localhost", port);
        URL url = URI.create("https://mail.grajagan.org/").toURL();
        server.createContext("/", new ProxyHandler(url));
        server.start();
        while (true) {
        }
    }

}
