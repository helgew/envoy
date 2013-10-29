package org.grajagan.envoy;

import static java.net.HttpURLConnection.HTTP_OK;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;

import javax.net.ssl.HttpsURLConnection;

import org.grajagan.ssl.HttpsServerFactoryTest;
import org.grajagan.ssl.HttpsURLConnectionFactory;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpsServer;

public class ProxyHandlerTest {

    private static final String TEST_PATH = "/search?q=test&qscrl=1";
    private ProxyHandler proxyHandler;
    private URL url;

    @Before
    public void setUp() throws MalformedURLException {
        url = new URL("https://www.google.com");
        proxyHandler = new ProxyHandler(url);
    }

    @After
    public void breakDown() {

    }

    @Test
    public void testGetRemoteConnection() throws Exception {
        GetRemoteConnectionTestHandler handler =
                new GetRemoteConnectionTestHandler(proxyHandler, url);
        HttpsServer server = HttpsServerFactoryTest.getTestServer(handler);
        server.start();
        URI uri =
                URI.create("https://" + server.getAddress().getHostName() + ":"
                        + server.getAddress().getPort() + TEST_PATH);
        HttpsURLConnection connection =
                HttpsURLConnectionFactory.createHttpsURLConnection(uri.toURL());
        connection.getResponseCode();
        assertEquals(null, handler.getErrorMessage());
        server.stop(0);
    }

    class GetRemoteConnectionTestHandler implements HttpHandler {

        private ProxyHandler proxyHandler;
        private URL remoteURL;
        private String errorMessage = null;

        public GetRemoteConnectionTestHandler(ProxyHandler proxyHandler, URL remoteURL) {
            this.proxyHandler = proxyHandler;
            this.remoteURL = remoteURL;
        }

        @Override
        public void handle(HttpExchange exchange) throws IOException {
            HttpsURLConnection connection = proxyHandler.getRemoteConnection(exchange);
            try {
                assertTrue(connection.getConnectTimeout() == ProxyHandler.TIMEOUT);
                assertTrue(connection.getDoInput());
                assertTrue(connection.getDoOutput());
                assertTrue(connection.getRequestMethod().equals("GET"));
                URL url = connection.getURL();
                assertEquals(remoteURL.getHost(), url.getHost());
                assertEquals(TEST_PATH, url.getPath() + "?" + url.getQuery());
            } catch (AssertionError e) {
                StringWriter sw = new StringWriter();
                PrintWriter pw = new PrintWriter(sw);
                e.printStackTrace(pw);
                setErrorMessage(sw.toString());
            }
            exchange.sendResponseHeaders(HTTP_OK, 0);
            exchange.close();
        }

        public String getErrorMessage() {
            return errorMessage;
        }

        public void setErrorMessage(String errorMessage) {
            this.errorMessage = errorMessage;
        }
    }
}
