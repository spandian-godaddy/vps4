package com.godaddy.vps4.util;

import static org.mockito.Mockito.mock;

import java.security.PrivateKey;
import java.security.cert.X509Certificate;

import javax.net.ssl.KeyManager;

import org.junit.Assert;
import org.junit.Test;

import com.godaddy.hfs.config.Config;
import com.godaddy.vps4.config.Configs;
import com.godaddy.vps4.util.KeyManagerBuilder.CertificateKeyManager;

public class KeyManagerBuilderTest {
    private static final String CLIENT_CERTIFICATE_KEY_PATH = "messaging.api.keyPath";
    private static final String CLIENT_CERTIFICATE_PATH = "messaging.api.certPath";

    @Test
    public void testGetCertificateChain() {
        X509Certificate certificate = mock(X509Certificate.class);
        X509Certificate[] mockCertificates = new X509Certificate[]{certificate};
        PrivateKey mockPrivateKey = mock(PrivateKey.class);
        CertificateKeyManager keyManager = new CertificateKeyManager(mockCertificates, mockPrivateKey);

        Assert.assertEquals(mockCertificates[0], keyManager.getCertificateChain(null)[0]);
    }

    @Test
    public void testGetPrivateKey() {
        X509Certificate certificate = mock(X509Certificate.class);
        X509Certificate[] mockCertificates = new X509Certificate[]{certificate};
        PrivateKey mockPrivateKey = mock(PrivateKey.class);
        CertificateKeyManager keyManager = new CertificateKeyManager(mockCertificates, mockPrivateKey);

        Assert.assertEquals(mockPrivateKey, keyManager.getPrivateKey(null));
    }

    @Test
    public void testNewKeyManagerReturnsCertificateKeyManager() {
        Config config = Configs.getInstance();

        KeyManager keyManager = KeyManagerBuilder.newKeyManager(
                config,
                CLIENT_CERTIFICATE_KEY_PATH,
                CLIENT_CERTIFICATE_PATH);
        Assert.assertNotNull(keyManager);
        Assert.assertEquals(CertificateKeyManager.class, keyManager.getClass());
    }

    @Test
    public void testChooseClientAlias() {
        Config config = Configs.getInstance();

        CertificateKeyManager keyManager = (CertificateKeyManager)KeyManagerBuilder.newKeyManager(
                config,
                CLIENT_CERTIFICATE_KEY_PATH,
                CLIENT_CERTIFICATE_PATH);
        Assert.assertEquals("vertical-client", keyManager.chooseClientAlias(null, null, null));
    }

    @Test
    public void testChooseServerAlias() {
        Config config = Configs.getInstance();

        CertificateKeyManager keyManager = (CertificateKeyManager)KeyManagerBuilder.newKeyManager(
                config,
                CLIENT_CERTIFICATE_KEY_PATH,
                CLIENT_CERTIFICATE_PATH);
        Assert.assertNull(keyManager.chooseServerAlias(null, null, null));
    }

    @Test
    public void testGetClientAliases() {
        Config config = Configs.getInstance();

        CertificateKeyManager keyManager = (CertificateKeyManager)KeyManagerBuilder.newKeyManager(
                config,
                CLIENT_CERTIFICATE_KEY_PATH,
                CLIENT_CERTIFICATE_PATH);
        Assert.assertNull(keyManager.getClientAliases(null, null));
    }

    @Test
    public void testGetServerAliases() {
        Config config = Configs.getInstance();

        CertificateKeyManager keyManager = (CertificateKeyManager)KeyManagerBuilder.newKeyManager(
                config,
                CLIENT_CERTIFICATE_KEY_PATH,
                CLIENT_CERTIFICATE_PATH);
        Assert.assertNull(keyManager.getServerAliases(null, null));
    }
}
