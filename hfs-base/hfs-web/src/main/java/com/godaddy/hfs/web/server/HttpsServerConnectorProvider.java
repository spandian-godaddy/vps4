package com.godaddy.hfs.web.server;

import java.io.IOException;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;

import javax.inject.Inject;
import javax.inject.Provider;
import javax.net.ssl.KeyManager;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;

import org.eclipse.jetty.http.HttpVersion;
import org.eclipse.jetty.server.HttpConfiguration;
import org.eclipse.jetty.server.HttpConnectionFactory;
import org.eclipse.jetty.server.SecureRequestCustomizer;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.server.SslConnectionFactory;
import org.eclipse.jetty.util.ssl.SslContextFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.godaddy.hfs.config.Config;
import com.godaddy.hfs.web.ServerConnectorProviderBase;

public class HttpsServerConnectorProvider extends ServerConnectorProviderBase implements Provider<ServerConnector> {

    private static final Logger logger = LoggerFactory.getLogger(HttpsServerConnectorProvider.class);

    static final String HTTPS_PORT_START = "8443";
    
    private final Config config;
    private final Server server;
    private final KeyManagerFactory keyManagerFactory;
	private final TrustManagerFactory trustManagerFactory;
	
	private final HttpConfiguration httpsConfig;

    @Inject
    public HttpsServerConnectorProvider(Config config, Server server, KeyManagerFactory keyManagerFactory, TrustManagerFactory trustManagerFactory) {
        this.config = config;
        this.server = server;
        this.trustManagerFactory = trustManagerFactory;
        this.keyManagerFactory = keyManagerFactory;
        
        httpsConfig = new HttpConfiguration();
        httpsConfig.setSecureScheme("https");
        httpsConfig.setSendServerVersion(false);
        httpsConfig.setSendDateHeader(false);
        httpsConfig.addCustomizer(new SecureRequestCustomizer());
    }

    @Override
    public ServerConnector get() {
        
        SslContextFactory sslContextFactory = new SslContextFactory();
        sslContextFactory.setIncludeProtocols("TLSv1.2");

        sslContextFactory.setNeedClientAuth(true);
        sslContextFactory.setExcludeCipherSuites(
                "SSL_RSA_WITH_DES_CBC_SHA",
                "SSL_DHE_RSA_WITH_DES_CBC_SHA",
                "SSL_DHE_DSS_WITH_DES_CBC_SHA",
                "SSL_RSA_EXPORT_WITH_RC4_40_MD5",
                "SSL_RSA_EXPORT_WITH_DES40_CBC_SHA",
                "SSL_DHE_RSA_EXPORT_WITH_DES40_CBC_SHA",
                "SSL_DHE_DSS_EXPORT_WITH_DES40_CBC_SHA");
        
        sslContextFactory.setSslContext(createSSLContext());
        
        SslConnectionFactory sslConnectionFactory = new SslConnectionFactory(sslContextFactory, HttpVersion.HTTP_1_1.asString());
        
        ServerConnector httpsConnector = new ServerConnector(
                server,
                sslConnectionFactory,
                new HttpConnectionFactory(httpsConfig));
        
        httpsConnector.setIdleTimeout(500000);

        int startingPort = Integer.parseInt(config.get("https.port", HTTPS_PORT_START));
        autoBindPort(httpsConnector, startingPort);

        return httpsConnector;
    }

    private SSLContext createSSLContext() {
        TrustManager[] trustManagers = trustManagerFactory.getTrustManagers();
        KeyManager[] keyManagers = keyManagerFactory.getKeyManagers();

        SSLContext sslContext;
        
        try {
            sslContext = SSLContext.getInstance("TLSv1.2");
            sslContext.init(keyManagers, trustManagers, new SecureRandom());
            
        } catch (NoSuchAlgorithmException | KeyManagementException e) {
            throw new IllegalStateException("Building SSLContext failed: " + e.getMessage(), e);
        }

        return sslContext;
    }

}