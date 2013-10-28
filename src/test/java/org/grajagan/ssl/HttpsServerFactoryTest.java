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

    @Test
    public void testHttpsServer() throws Exception {
        final String address = "localhost";
        final int port = 9999;
        HttpsServer server = HttpsServerFactory.createServer(address, port);
        server.createContext("/", new EmptyHandler());
        server.start();
        URI uri = URI.create("https://" + address + ":" + port + "/");
        HttpsURLConnection.setDefaultHostnameVerifier(new javax.net.ssl.HostnameVerifier() {
            public boolean verify(String hostname, javax.net.ssl.SSLSession sslSession) {
                if (hostname.equals(address)) {
                    return true;
                }
                return false;
            }
        });
        HttpsURLConnection connection =
                HttpsURLConnectionFactory.createHttpsURLConnection(uri.toURL());
        assertTrue(connection.getResponseCode() == HTTP_OK);
    }
}

class EmptyHandler implements HttpHandler {
    @Override
    public void handle(HttpExchange httpExchange) throws IOException {
        httpExchange.sendResponseHeaders(HTTP_OK, 0);
        httpExchange.close();
    }
}