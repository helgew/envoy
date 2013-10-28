package org.grajagan.ssl;

import java.net.URL;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManagerFactory;

/**
 * Factory class to create HttpsURLConnection objects using our own keystore.
 */
public final class HttpsURLConnectionFactory {

    /** Factory class constructor. */
    private HttpsURLConnectionFactory() {
    }

    /**
     * Create a HttpsURLConnection to the given URL using our own keystore.
     * 
     * @param url
     *            the URL to connect to
     * @return an HttpsURLConnection
     * @throws Exception
     *             if there is any error
     */
    public static HttpsURLConnection createHttpsURLConnection(URL url) throws Exception {
        HttpsURLConnection conn = (HttpsURLConnection) url.openConnection();
        TrustManagerFactory tmf = KeyStoreHelper.getTrustManagerFactory();
        SSLContext ctx = SSLContext.getInstance("SSLv3");
        ctx.init(null, tmf.getTrustManagers(), null);
        SSLSocketFactory sslFactory = ctx.getSocketFactory();
        conn.setSSLSocketFactory(sslFactory);

        return conn;
    }
}
