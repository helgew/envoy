package org.grajagan.ssl;

import static java.net.HttpURLConnection.HTTP_OK;
import static org.junit.Assert.assertTrue;

import java.net.URI;

import javax.net.ssl.HttpsURLConnection;

import org.junit.Test;

public class HttpsURLConnectionFactoryTest {

    @Test
    public void testConnectivity() throws Exception {
        URI uri = URI.create("https://mail.grajagan.org/");
        HttpsURLConnection connection =
                HttpsURLConnectionFactory.createHttpsURLConnection(uri.toURL());
        assertTrue(connection.getResponseCode() == HTTP_OK);
    }
}
