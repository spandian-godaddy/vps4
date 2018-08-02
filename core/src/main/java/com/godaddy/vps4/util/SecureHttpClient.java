package com.godaddy.vps4.util;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.security.SecureRandom;
import java.util.Map;

import javax.net.ssl.KeyManager;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;

import org.apache.http.Header;
import org.apache.http.HttpResponse;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.config.RegistryBuilder;
import org.apache.http.conn.socket.ConnectionSocketFactory;
import org.apache.http.conn.socket.PlainConnectionSocketFactory;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.apache.http.message.BasicHeader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.godaddy.hfs.config.Config;

public class SecureHttpClient {

    private static final Logger logger = LoggerFactory.getLogger(SecureHttpClient.class);

    protected final CloseableHttpClient client;

    private static final ObjectMapper payloadMapper = new ObjectMapper();

    private Config config;
    private String clientCertKeyPath;
    private String clientCertPath;


    public SecureHttpClient(Config config, String clientCertKeyPath, String clientCertPath) {
        this.config = config;
        this.clientCertKeyPath = clientCertKeyPath;
        this.clientCertPath = clientCertPath;

        payloadMapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
        payloadMapper.configure(DeserializationFeature.ACCEPT_EMPTY_ARRAY_AS_NULL_OBJECT, true);
        payloadMapper.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);

        client = createHttpClient();
    }

    public <T> T executeHttp(HttpUriRequest request, Class<T> deserializeInto) throws IOException {
        logger.debug(String.format("Calling api: %s", request.getURI()));
        HttpResponse response = client.execute(request);
        return deserializeResponse(response.getEntity().getContent(), deserializeInto);
    }

    protected <T> T deserializeResponse(InputStream inputStream, Class<T> deserializeInto) {
        try {
            T deserialized = payloadMapper.readValue(new InputStreamReader(inputStream, "UTF-8"), deserializeInto);
            return deserialized;
        } catch(Exception e) {
            logger.error("deserializeResponse Exception.", e);
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }

    CloseableHttpClient createHttpClient() {
        try {
            SSLContext sslContext = SSLContext.getInstance("TLSv1.2");
            KeyManager[] kms = { KeyManagerBuilder.newKeyManager(config, clientCertKeyPath, clientCertPath) };
            TrustManager[] tms = { DefaultHttpClient.TRUST_MANAGER };
            sslContext.init(kms, tms, new SecureRandom());

            SSLConnectionSocketFactory socketFactory = new SSLConnectionSocketFactory(
                    sslContext,
                    null,
                    null,
                    SSLConnectionSocketFactory.ALLOW_ALL_HOSTNAME_VERIFIER);

            PoolingHttpClientConnectionManager connPool = new PoolingHttpClientConnectionManager(RegistryBuilder.<ConnectionSocketFactory>create()
                    .register("http", PlainConnectionSocketFactory.getSocketFactory())
                    .register("https", socketFactory)
                    .build());

            connPool.setMaxTotal(500);
            connPool.setDefaultMaxPerRoute(250);

            RequestConfig requestConfig = RequestConfig.custom()
                    .setConnectionRequestTimeout(5000)
                    .setConnectTimeout(5000)
                    .setSocketTimeout(20000).build();

            CloseableHttpClient httpclient = HttpClients.custom()
                    .setDefaultRequestConfig(requestConfig)
                    .setConnectionManager(connPool)
                    .build();

            return httpclient;
        }
        catch(Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static Header[] getDefaultJsonHeaders() {
        Header[] defaultHeaders = {
                new BasicHeader("Content-type", "application/json"),
                new BasicHeader("Accept", "application/json")
        };
        return defaultHeaders;
    }

    public static HttpPost createJsonHttpPostWithHeaders(String uri, Map<String, String> headersToAdd){
        HttpPost httpPost = new HttpPost(uri);
        httpPost.setHeaders(getDefaultJsonHeaders());

        if(headersToAdd != null){
            for (Map.Entry<String, String> me : headersToAdd.entrySet()) {
                httpPost.setHeader(me.getKey(), me.getValue());
            }
        }

        return httpPost;
    }

    public static HttpGet createJsonHttpGet(String uri){
        HttpGet httpGet = new HttpGet(uri);
        httpGet.setHeaders(getDefaultJsonHeaders());

        return httpGet;
    }

    public static String createJSONStringFromObject(Object o) {
        try {
            return payloadMapper.writeValueAsString(o);
        } catch (JsonProcessingException e) {
            logger.error("Exception while converting json to string : ", e);
            throw new RuntimeException(e);
        }
    }
}
