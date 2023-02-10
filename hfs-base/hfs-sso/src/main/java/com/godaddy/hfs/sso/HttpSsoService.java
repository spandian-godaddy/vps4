package com.godaddy.hfs.sso;

import java.io.IOException;
import java.io.InputStream;
import java.text.ParseException;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import com.nimbusds.jwt.SignedJWT;
import net.minidev.json.JSONArray;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.nimbusds.jose.jwk.JWK;

import net.minidev.json.JSONObject;
import net.minidev.json.JSONValue;

/**
 *
 * A service that calls the sso endpoint to gather information about user credentials
 *
 */
public class HttpSsoService implements SsoService {
    /** fetches keys from the single-sign-on service and caches with a 24-hour TTL
     *
     * Reference: https://confluence.int.godaddy.com/display/AUTH/Integration+Guide# IntegrationGuide-Forsame-domainwebapps
     *
     * @author Brian Diekelman
     *
     */

    private static final long KEY_TTL = 24 * 60 * 60 * 1000; // 24 hours

    private static final Logger logger = LoggerFactory.getLogger(HttpSsoService.class);

    /**
     * httpClient = HttpClientBuilder.create().setMaxConnPerRoute(50).setMaxConnTotal(50).build();
     */
    private final CloseableHttpClient client;

    /**
     * Should look something like: https://sso.(dev-|test-|stg-|)godaddy.com/
     */
    private final String ssoBaseUrl;

    private final ConcurrentHashMap<String, CachedKey> keyCache = new ConcurrentHashMap<>();

    public HttpSsoService(String ssoBaseUrl, CloseableHttpClient client) {
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

        JSONObject keyJson = callSso(get);

        try {
            return JWK.parse(keyJson);
        }
        catch(ParseException e){
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

    public List<String> getGroups(SignedJWT jwt) {
        /**
         * Fetch a list of AD-Groups that the SignedJWT user belongs to.
         *
         * @author George Sheppard
         */

        logger.info("fetching groups from SSO: {}", jwt.getParsedString());

        HttpGet get = new HttpGet(ssoBaseUrl + "api/my/ad_membership");
        get.addHeader("Authorization", "sso-jwt " + jwt.getParsedString());

        JSONObject ssoData = callSso(get);

        JSONArray groups = (JSONArray) ssoData.get("groups");

        if(groups == null){
            throw new RuntimeException("Could not find 'groups' in sso response.");
        }

        return groups.stream().map(Object::toString).collect(Collectors.toList());

    }

    private JSONObject callSso(HttpGet httpGet ){
        try{
            CloseableHttpResponse response = client.execute(httpGet);
            try {
                int status = response.getStatusLine().getStatusCode();
                if (status == 404) {
                    throw new IOException("SSO token signed by unknown key");
                }
                if (status != 200) {
                    throw new IOException("Unable to fetch key from key service (server returned status  " + status + ")");
                }

                // FIXME more resilient error handling here... bad response, etc
                InputStream is = response.getEntity().getContent();
                try {
                    JSONObject jsonValue = (JSONObject) JSONValue.parse(is);

                    JSONObject data = (JSONObject) jsonValue.get("data");

                    if(data == null){
                        throw new RuntimeException("Could not find 'data' in sso response.");
                    }

                    return data;
                }
                finally {
                    is.close();
                }
            }
            finally {
                response.close();
            }
        }
        catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

}
