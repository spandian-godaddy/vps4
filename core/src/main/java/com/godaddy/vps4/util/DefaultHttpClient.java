package com.godaddy.vps4.util;

import java.security.SecureRandom;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;

import javax.net.ssl.KeyManager;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import org.apache.http.client.HttpClient;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;

public class DefaultHttpClient {

    static final X509TrustManager TRUST_MANAGER = new X509TrustManager() {

        @Override
        public void checkClientTrusted(X509Certificate[] arg0, String arg1)
                throws CertificateException {

        }

        @Override
        public void checkServerTrusted(X509Certificate[] arg0, String arg1)
                throws CertificateException {

        }

        @Override
        public X509Certificate[] getAcceptedIssuers() {
            return null;
        }

    };

    private static final CloseableHttpClient httpClient = initHttpClient();

    static CloseableHttpClient initHttpClient() {
        try {
            SSLContext sslContext = SSLContext.getInstance("TLSv1.2");
            sslContext.init(
                    new KeyManager[] {},
                    new TrustManager[] { TRUST_MANAGER },
                    new SecureRandom());

            return  HttpClientBuilder.create()
                    // TODO configure
                    .setSslcontext(sslContext)
                    .setHostnameVerifier(SSLConnectionSocketFactory.ALLOW_ALL_HOSTNAME_VERIFIER)
                    .disableRedirectHandling()
                    .setMaxConnPerRoute(250)
                    .setMaxConnTotal(500)
                    .build();
        }
        catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static HttpClient get() {
        return httpClient;
    }

}
