package org.grajagan.ssl;

import java.io.InputStream;
import java.security.KeyStore;
import java.security.KeyStoreException;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.TrustManagerFactory;

import org.apache.log4j.Logger;

public final class KeyStoreHelper {

    private static final Logger LOG = Logger.getLogger(KeyStoreHelper.class);
    private static final String KEYSTORE_NAME = "/keystore";
    private static final char[] KEYSTORE_PASS = "k5pa55w0rd".toCharArray();
    private static final char[] KEY_PASSWORD = "kpa55w0rd".toCharArray();

    private KeyStoreHelper() {
    }

    public static KeyStore getKeyStore() throws KeyStoreException {
        InputStream is = HttpsServerFactory.class.getResourceAsStream(KEYSTORE_NAME);
        if (is == null) {
            throw new KeyStoreException("Cannot find keystore " + KEYSTORE_NAME);
        }

        KeyStore ks = null;
        try {
            ks = KeyStore.getInstance(KeyStore.getDefaultType());
            ks.load(is, KEYSTORE_PASS);
        } catch (Exception e) {
            LOG.error("Cannot load KeyStore", e);
            throw new KeyStoreException(e);
        }

        return ks;
    }

    public static KeyManagerFactory getKeyManagerFactory() throws KeyStoreException {
        KeyManagerFactory kmf = null;
        KeyStore ks = getKeyStore();
        try {
            kmf = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
            kmf.init(ks, KEY_PASSWORD);
        } catch (Exception e) {
            LOG.error("Cannot initialize KeyManagerFactory", e);
            throw new KeyStoreException(e);
        }

        return kmf;
    }

    public static TrustManagerFactory getTrustManagerFactory() throws KeyStoreException {
        KeyStore keyStore = getKeyStore();
        TrustManagerFactory tmf = null;
        try {
            tmf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
            tmf.init(keyStore);
        } catch (Exception e) {
            LOG.error("Cannot initialize TrustManagerFactory", e);
            throw new KeyStoreException(e);
        }

        return tmf;
    }
}
