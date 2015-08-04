package org.grajagan.ssl;

import static org.junit.Assert.assertNotNull;

import java.security.KeyStore;
import java.security.KeyStoreException;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.TrustManagerFactory;

import org.junit.Test;

public class KeyStoreHelperTest {

    @Test
    public void testGetKeyStore() throws KeyStoreException {
        KeyStore ks = KeyStoreHelper.getKeyStore();
        assertNotNull(ks);
    }

    @Test
    public void testGetKeyManagerFactory() throws KeyStoreException {
        KeyManagerFactory kmf = KeyStoreHelper.getKeyManagerFactory();
        assertNotNull(kmf);
    }

    @Test
    public void testGetTrustManagerFactory() throws KeyStoreException {
        TrustManagerFactory tmf = KeyStoreHelper.getTrustManagerFactory();
        assertNotNull(tmf);
    }

}
