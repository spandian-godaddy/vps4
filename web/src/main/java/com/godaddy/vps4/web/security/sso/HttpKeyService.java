package com.godaddy.vps4.web.security.sso;

import java.io.IOException;
import java.io.InputStream;
import java.text.ParseException;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.godaddy.vps4.security.jdbc.AuthorizationException;
import com.nimbusds.jose.jwk.JWK;

import net.minidev.json.JSONObject;
import net.minidev.json.JSONValue;

/**
 * A service that fetches keys from the single-sign-on service and caches with a 24-hour TTL
 *
 * Reference: https://confluence.int.godaddy.com/display/AUTH/Integration+Guide# IntegrationGuide-Forsame-domainwebapps
 *
 * @author Brian Diekelman
 *
 */
public class HttpKeyService implements KeyService {
    private static final long KEY_TTL = 24 * 60 * 60 * 1000; // 24 hours

    private static final Logger logger = LoggerFactory.getLogger(HttpKeyService.class);

    /**
     * httpClient = HttpClientBuilder.create().setMaxConnPerRoute(50).setMaxConnTotal(50).build();
     */
    private final CloseableHttpClient client;

    /**
     * Should look something like: https://sso.(dev-|test-|stg-|)godaddy.com/
     */
    private final String ssoBaseUrl;

    private final ConcurrentHashMap<String, CachedKey> keyCache = new ConcurrentHashMap<>();

    public HttpKeyService(String ssoBaseUrl, CloseableHttpClient client) {
        this.client = client;

        if (!ssoBaseUrl.endsWith("/")) {
            ssoBaseUrl += '/';
        }
        this.ssoBaseUrl = ssoBaseUrl;
    }

    @Override
    public JWK getKey(String keyId) {
        CachedKey cachedKey = keyCache.get(keyId);
        if (cachedKey != null) {
            // see if the key is expired
            long now = System.currentTimeMillis();

            long age = (now - cachedKey.cachedTime);

            if (age < KEY_TTL) {
                logger.debug("key {} still valid", keyId);
                // cached key is still good
                return cachedKey.key;
            }
            logger.debug("key {} expired, fetching new key", keyId);

            // otherwise key is too old, pretend like we never found one
            cachedKey = null;
        }

        // note: multiple threads could both fetch the key at the same time...
        // just let the last one win by doing an unconditional put()
        // once the key is fetched... should happen once every 24 hours
        // so not worth sychronization
        //
        JWK key = fetchKey(keyId);

        keyCache.put(keyId, new CachedKey(key, System.currentTimeMillis()));

        return key;
    }

    protected JWK fetchKey(String keyId) {
        // fetch the key: https://sso.godaddy.com/v1/api/key/C4W65gHzHw
        // https://sso.godaddy.com/v1/api/key/{keyId}

        logger.info("fetching key from SSO: {}", keyId);

        HttpGet get = new HttpGet(ssoBaseUrl + "v1/api/key/" + keyId);

        try {
            CloseableHttpResponse response = client.execute(get);
            try {
                int status = response.getStatusLine().getStatusCode();
                if (status == 404) {
                    throw new AuthorizationException("SSO token signed by unknown key");
                }
                if (status != 200) {
                    throw new IOException("Unable to fetch key from key service (server returned status  " + status + ")");
                }

                // FIXME more resilient error handling here... bad response, etc
                InputStream is = response.getEntity().getContent();
                try {
                    JSONObject jsonValue = (JSONObject) JSONValue.parse(is);

                    JSONObject keyJson = (JSONObject) jsonValue.get("data");

                    return JWK.parse(keyJson);
                }
                finally {
                    is.close();
                }
            }
            finally {
                response.close();
            }
        }
        catch (IOException | ParseException e) {
            throw new RuntimeException(e);
        }
    }

    private static class CachedKey {
        final JWK key;

        final long cachedTime;

        public CachedKey(JWK key, long cachedTime) {
            this.key = key;
            this.cachedTime = cachedTime;
        }
    }

}
