package org.grajagan.ssl;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.CertificateExpiredException;
import java.security.cert.CertificateNotYetValidException;
import java.security.cert.X509Certificate;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.TrustManagerFactory;

import org.apache.log4j.Logger;
import org.grajagan.envoy.EnvoyProxyServer;

public final class KeyStoreHelper {

    private static final Logger LOG = Logger.getLogger(KeyStoreHelper.class);
    private static final String KEYSTORE_NAME = "/keystore";
    private static final char[] KEYSTORE_PASS = "k5pa55w0rd".toCharArray();
    private static final char[] KEY_PASSWORD = "kpa55w0rd".toCharArray();
    
    private static KeyStore keyStore;

    private KeyStoreHelper() {
    }

    public static KeyStore getKeyStore() throws KeyStoreException {
        if (keyStore != null) {
            return keyStore;
        }
        
        keyStore = getKeyStoreFromResources();
        
        Certificate[] certs = null;
        try {
            URL url = new URL(EnvoyProxyServer.DEFAULT_REMOTE_URL);
            HttpsURLConnection conn = (HttpsURLConnection) url.openConnection();
            conn.connect();
            certs = conn.getServerCertificates();
        } catch (IOException e) {
            LOG.error("Cannot retrieve certificates from remote server!", e);
        }
        
        if (certs == null) {
	    LOG.warn("Did not get any certs from remote!");
            return getKeyStoreFromResources();
        }
        
        int i = 1;
        for (Certificate cert : certs) {
            if (cert instanceof X509Certificate) {
                try {
                    ((X509Certificate) cert).checkValidity();
                    keyStore.setCertificateEntry("alias" + i++, cert);
                } catch (CertificateExpiredException | CertificateNotYetValidException e) {
                    LOG.warn("Certificate is expired or not yet valid", e);
                }
            } else {
                LOG.warn("Unknown certificate type: " + cert);
            }
        }
        
        return keyStore;
    }
    
    private static KeyStore getEmptyKeyStore() {
        KeyStore ks = null;

        try {
            ks = KeyStore.getInstance("jks");
            ks.load(null, KEYSTORE_PASS);
        } catch (KeyStoreException | NoSuchAlgorithmException | CertificateException | IOException e) {
            e.printStackTrace();
        }

        return ks;
    }

    public static KeyStore getKeyStoreFromResources() throws KeyStoreException {
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
