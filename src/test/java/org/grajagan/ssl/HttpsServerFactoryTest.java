package org.grajagan.ssl;

import static java.net.HttpURLConnection.HTTP_OK;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.net.URI;

import javax.net.ssl.HttpsURLConnection;

import org.junit.Test;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpsServer;

public class HttpsServerFactoryTest {

    public final static String BIND_ADDRESS = "localhost";
    public final static int PORT = 9999;

    @Test
    public void testHttpsServer() throws Exception {
        HttpHandler handler = new EmptyHandler();
        HttpsServer server = getTestServer(handler);
        server.start();
        URI uri = URI.create("https://" + BIND_ADDRESS + ":" + PORT + "/");
        HttpsURLConnection connection =
                HttpsURLConnectionFactory.createHttpsURLConnection(uri.toURL());
        assertTrue(connection.getResponseCode() == HTTP_OK);
    }

    public static HttpsServer getTestServer(HttpHandler handler) throws Exception {
        HttpsServer server = HttpsServerFactory.createServer(BIND_ADDRESS, PORT);
        server.createContext("/", handler);
        HttpsURLConnection.setDefaultHostnameVerifier(new javax.net.ssl.HostnameVerifier() {
            public boolean verify(String hostname, javax.net.ssl.SSLSession sslSession) {
                if (hostname.equals(BIND_ADDRESS)) {
                    return true;
                }
                return false;
            }
        });
        return server;
    }
}

class EmptyHandler implements HttpHandler {
    @Override
    public void handle(HttpExchange httpExchange) throws IOException {
        httpExchange.sendResponseHeaders(HTTP_OK, 0);
        httpExchange.close();
    }
}