package com.godaddy.vps4.security.sso;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import java.security.interfaces.RSAPublicKey;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;

import org.json.simple.JSONObject;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.mockito.Mockito;

import com.godaddy.vps4.security.crypto.KeyPairUtil;
import com.godaddy.vps4.security.crypto.RSAKeyPair;
import com.godaddy.vps4.web.security.sso.KeyService;
import com.godaddy.vps4.web.security.sso.SsoTokenExtractor;
import com.godaddy.vps4.web.security.sso.token.SsoToken;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.JWSSigner;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jose.jwk.JWK;
import com.nimbusds.jose.jwk.KeyUse;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.util.Base64URL;
import com.nimbusds.jwt.JWT;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.ReadOnlyJWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;

public class SsoTokenExtractorTest {

    // examples of SSO tokens pulled from dev environment cookies and keys from
    // the key service (for reference)
    // static final String SSO_TOKEN =
    // "eyJhbGciOiAiUlMyNTYiLCAia2lkIjogIkl0WTRlOXNOT3cifQ.eyJ1c2VybmFtZSI6ICI1eTUiLCAiZmlyc3RuYW1lIjogIkJyaWFuIiwgImZhY3RvcnMiOiB7ImtfcHciOiAxNDU0NjIwMzQ5fSwgImxhc3RuYW1lIjogIkRpZWtlbG1hbiIsICJhdXRoIjogImJhc2ljIiwgImZ0YyI6IDEsICJzaG9wcGVySWQiOiAiNXk1IiwgImp0aSI6ICI3YVFQelI2eHRxa0JvRFAzRGprUDR3IiwgInBsaWQiOiAiMSIsICJpYXQiOiAxNDU0NjIwMzQ5LCAidHlwIjogImlkcCJ9.aXW0_AYK3IdB0YRI9LDqL2N0hqM02zNf6JFdPElMllRxWls7b6QYgljgek3yRooYVZ2BOLLZbX8CEailbC5q5L3m6m-Yp9vtzvxzNqnPkPeCs4NUO-KzWu_60tIpfBeOvogWMtnxTBrTKDowQM6ahK1-Z-EFjsa_AS0_iZOeuCE";
    // static final String KEY_JSON = "{\"kid\": \"ItY4e9sNOw\", \"e\":
    // \"AQAB\", \"kty\": \"RSA\", \"n\":
    // \"wK-ePbapCOYPaE9g1P19Dq3cTXzLmxPQe4J8XOD5TR4nbab9ValqSQ-74BAFshoNfr2I6nR34xvfh8ORy3UWhiXpTgAqoM31PlvFOFck3XK2boHz9P3SaTITjAN2sXx3Fp7k2MyUAsxeA2ZIFT-V84QvMPITuOwwdPu3FrkGIeE\"}";

    private static final String USERNAME = "bdiekelman";

    // private static final int DEL_USER_ID = 2;
    private static final String DEL_SHOPPER_ID = "922112";
    private static final String DEL_USERNAME = "aaherrera";

    // private static final int SHOPPER_USER_ID = 3;
    private static final String SHOPPER_ID = "922772";
    private static final String SHOPPER_USERNAME = "jiwen";

    // private static final int EMPLOYEE_USER_ID = 4;
    private static final String EMPLOYEE_USERNAME = "employeeUsername";

    static RSAKeyPair keyPair;

    static JWK jwk;

    static final String KEY_ID = "asdf1234";

    @BeforeClass
    public static void generateKeyPair() throws Exception {
        keyPair = KeyPairUtil.generate();

        jwk = newJWK(keyPair.getPublicKey(), KEY_ID);
    }

    SsoTokenExtractor ssoTokenExtractor;

    @Before
    public void initServices() {
        KeyService keyService = Mockito.mock(KeyService.class);
        Mockito.when(keyService.getKey(KEY_ID)).thenReturn(jwk);

        ssoTokenExtractor = new SsoTokenExtractor(keyService);
    }

    // SHOPPER TO SHOPPER
    static JWT newS2SJWT(long iat) throws Exception {

        JWSHeader header = new JWSHeader.Builder(JWSAlgorithm.RS256).keyID(KEY_ID).build();

        iat /= 1000; // iat is in seconds, not milliseconds (convert ms -> sec)
        String payloadJson = newS2SImpersonationPayload(iat).toJSONString();
        ReadOnlyJWTClaimsSet claims = JWTClaimsSet.parse(payloadJson);

        SignedJWT jwt = new SignedJWT(header, claims);
        JWSSigner signer = new RSASSASigner(keyPair.getPrivateKey());

        jwt.sign(signer);

        return jwt;
    }

    // EPLOYEE TO SHOPPER
    static JWT newE2SJWT(long iat) throws Exception {

        JWSHeader header = new JWSHeader.Builder(JWSAlgorithm.RS256).keyID(KEY_ID).build();

        iat /= 1000; // iat is in seconds, not milliseconds (convert ms -> sec)
        String payloadJson = newE2SImpersonationPayload(iat).toJSONString();
        ReadOnlyJWTClaimsSet claims = JWTClaimsSet.parse(payloadJson);

        SignedJWT jwt = new SignedJWT(header, claims);
        JWSSigner signer = new RSASSASigner(keyPair.getPrivateKey());

        jwt.sign(signer);

        return jwt;
    }

    // EMPLOYEE TO SHOPPER TO SHOPPER
    static JWT newE2S2SJWT(long iat) throws Exception {

        JWSHeader header = new JWSHeader.Builder(JWSAlgorithm.RS256).keyID(KEY_ID).build();

        iat /= 1000; // iat is in seconds, not milliseconds (convert ms -> sec)
        String payloadJson = newE2S2SImpersonationPayload(iat).toJSONString();
        ReadOnlyJWTClaimsSet claims = JWTClaimsSet.parse(payloadJson);

        SignedJWT jwt = new SignedJWT(header, claims);
        JWSSigner signer = new RSASSASigner(keyPair.getPrivateKey());

        jwt.sign(signer);

        return jwt;
    }

    static JWT newJWT(long iat) throws Exception {

        JWSHeader header = new JWSHeader.Builder(JWSAlgorithm.RS256).keyID(KEY_ID).build();

        iat /= 1000; // iat is in seconds, not milliseconds (convert ms -> sec)
        String payloadJson = newPayload(iat).toJSONString();
        ReadOnlyJWTClaimsSet claims = JWTClaimsSet.parse(payloadJson);

        SignedJWT jwt = new SignedJWT(header, claims);
        JWSSigner signer = new RSASSASigner(keyPair.getPrivateKey());

        jwt.sign(signer);

        return jwt;
    }

    static JWK newJWK(RSAPublicKey publicKey, String keyId) {

        Base64URL n = Base64URL.encode(publicKey.getModulus());
        Base64URL e = Base64URL.encode(publicKey.getPublicExponent());

        JWK jwk = new RSAKey(n, e, KeyUse.SIGNATURE, null, null, keyId, null, null, null);

        return jwk;
    }

    static JSONObject newPayload(long iat) {

        JSONObject payload = new JSONObject();

        JSONObject factors = new JSONObject();
        factors.put("k_pw", 1438041184);
        payload.put("factors", factors);

        payload.put("firstname", "Brian");
        payload.put("lastname", "Diekelman");
        payload.put("auth", "basic");
        payload.put("ftc", 1);
        payload.put("jti", "");
        payload.put("iat", iat);
        payload.put("typ", "jomax");
        payload.put("accountName", USERNAME);

        List<String> groups = new ArrayList<>();
        groups.add("Toolzilla-HOC");
        groups.add("Development");
        payload.put("groups", groups);

        return payload;
    }

    static JSONObject newS2SImpersonationPayload(long iat) {

        JSONObject payload = new JSONObject();
        payload.put("auth", "s2s");

        JSONObject delegate = new JSONObject();
        delegate.put("auth", "basic");
        JSONObject factorsDel = new JSONObject();
        factorsDel.put("k_pw", 1438041184);
        delegate.put("factors", factorsDel);
        delegate.put("firstname", DEL_USERNAME);
        delegate.put("ftc", 1);
        delegate.put("hbi", iat);
        delegate.put("iat", iat);
        delegate.put("jti", "pp-PdDQad2qaxQrg0NSNuw");
        delegate.put("lastname", "Herrera");
        delegate.put("plid", "1");
        delegate.put("shopperId", DEL_SHOPPER_ID);
        delegate.put("typ", "idp");
        delegate.put("username", DEL_USERNAME);
        payload.put("del", delegate);

        payload.put("iat", iat);
        payload.put("jti", "M69YM39otMUC1PW12HdSFQ");

        JSONObject s2s = new JSONObject();
        s2s.put("al", 3);
        s2s.put("disp", "jace");
        s2s.put("firstname", "jace");
        s2s.put("ftc", 0);
        s2s.put("lastname", "iwen");
        s2s.put("plid", "1");
        s2s.put("shopperId", SHOPPER_ID);
        s2s.put("username", SHOPPER_USERNAME);
        payload.put("s2s", s2s);

        payload.put("typ", "idp");
        return payload;
    }

    static JSONObject newE2SImpersonationPayload(long iat) {

        JSONObject payload = new JSONObject();
        payload.put("auth", "e2s");

        JSONObject delegate = new JSONObject();
        delegate.put("auth", "basic");
        JSONObject factorsDel = new JSONObject();
        factorsDel.put("k_pw", 1438041184);
        delegate.put("factors", factorsDel);
        delegate.put("firstname", "employee");
        delegate.put("ftc", 1);
        delegate.put("iat", iat);
        delegate.put("jti", "pp-PdDQad2qaxQrg0NSNuw");
        delegate.put("lastname", "Herrera");
        delegate.put("typ", "jomax");
        delegate.put("accountName", EMPLOYEE_USERNAME);
        delegate.put("groups", "['DEV-SSO', 'Development'],");
        payload.put("del", delegate);

        payload.put("iat", iat);
        payload.put("jti", "M69YM39otMUC1PW12HdSFQ");

        JSONObject e2s = new JSONObject();
        e2s.put("firstname", "jace");
        e2s.put("ftc", 0);
        e2s.put("lastname", "iwen");
        e2s.put("plid", "1");
        e2s.put("shopperId", SHOPPER_ID);
        e2s.put("username", SHOPPER_USERNAME);
        e2s.put("ucid", "1234");
        payload.put("e2s", e2s);

        payload.put("typ", "idp");
        return payload;
    }

    static JSONObject newE2S2SImpersonationPayload(long iat) {

        JSONObject payload = new JSONObject();
        payload.put("auth", "e2s2s");
        payload.put("iat", iat);
        payload.put("jti", "");
        payload.put("typ", "idp");

        JSONObject outterDel = new JSONObject();
        outterDel.put("auth", "e2s");
        outterDel.put("iat", iat);
        outterDel.put("jti", "");
        outterDel.put("typ", "idp");

        JSONObject employee = new JSONObject();
        employee.put("auth", "basic");
        JSONObject factorsEmp = new JSONObject();
        factorsEmp.put("k_pw", 1438041184);
        employee.put("factors", factorsEmp);
        employee.put("firstname", "employee");
        employee.put("ftc", 1);
        employee.put("iat", iat);
        employee.put("jti", "");
        employee.put("lastname", "Herrera");
        employee.put("typ", "jomax");
        employee.put("accountName", EMPLOYEE_USERNAME);
        employee.put("groups", "['DEV-SSO', 'Development'],");
        outterDel.put("del", employee);

        JSONObject e2s = new JSONObject();
        e2s.put("firstname", "jace");
        e2s.put("ftc", 0);
        e2s.put("lastname", "iwen");
        e2s.put("plid", "1");
        e2s.put("shopperId", DEL_SHOPPER_ID);
        e2s.put("username", DEL_USERNAME);
        outterDel.put("e2s", e2s);
        payload.put("del", outterDel);

        JSONObject e2s2s = new JSONObject();
        e2s2s.put("al", 3);
        e2s2s.put("disp", "jace");
        e2s2s.put("firstname", "jace");
        e2s2s.put("ftc", 0);
        e2s2s.put("lastname", "iwen");
        e2s2s.put("plid", "1");
        e2s2s.put("shopperId", SHOPPER_ID);
        e2s2s.put("username", SHOPPER_USERNAME);
        payload.put("e2s2s", e2s2s);

        return payload;
    }

    @Test
    public void testAuthorizationAuthenticate() throws Exception {

        // create an unexpired JWT token
        JWT jwt = newJWT(Instant.now().toEpochMilli());
        HttpServletRequest request = Mockito.mock(HttpServletRequest.class);
        Mockito.when(request.getHeader("Authorization")).thenReturn("sso-jwt " + jwt.serialize());

        SsoToken token = ssoTokenExtractor.extractToken(request);
        assertNotNull(token);
    }

    // SHOPPER TO SHOPPER
    @Test
    public void testS2sAuthorizationAuthenticate() throws Exception {

        // create an unexpired JWT token
        JWT jwt = newS2SJWT(Instant.now().toEpochMilli());
        HttpServletRequest request = Mockito.mock(HttpServletRequest.class);
        Mockito.when(request.getHeader("Authorization")).thenReturn("sso-jwt " + jwt.serialize());

        SsoToken token = ssoTokenExtractor.extractToken(request);
        assertNotNull(token);
    }

    // Employee TO SHOPPER
    @Test
    public void testE2sAuthorizationAuthenticate() throws Exception {

        // create an unexpired JWT token
        JWT jwt = newE2SJWT(Instant.now().toEpochMilli());
        HttpServletRequest request = Mockito.mock(HttpServletRequest.class);
        Mockito.when(request.getHeader("Authorization")).thenReturn("sso-jwt " + jwt.serialize());

        SsoToken token = ssoTokenExtractor.extractToken(request);
        assertNotNull(token);
    }

    // EMPLOYEE TO SHOPPER TO SHOPPER
    @Test
    public void testE2s2sAuthorizationAuthenticate() throws Exception {

        // create an unexpired JWT token
        JWT jwt = newE2S2SJWT(Instant.now().toEpochMilli());
        HttpServletRequest request = Mockito.mock(HttpServletRequest.class);
        Mockito.when(request.getHeader("Authorization")).thenReturn("sso-jwt " + jwt.serialize());

        SsoToken token = ssoTokenExtractor.extractToken(request);
        assertNotNull(token);
    }

    @Test
    public void testCookieAuthenticate() throws Exception {

        // create an unexpired JWT token
        JWT jwt = newJWT(Instant.now().toEpochMilli());
        HttpServletRequest request = Mockito.mock(HttpServletRequest.class);
        Cookie[] cookies = { new Cookie("auth_idp", jwt.serialize()) };
        Mockito.when(request.getCookies()).thenReturn(cookies);

        SsoToken token = ssoTokenExtractor.extractToken(request);
        assertNotNull(token);
    }

    @Test
    public void testAuthorizationExpiredToken() throws Exception {

        // create an expired JWT token
        JWT jwt = newJWT(Instant.now().minusMillis(SsoTokenExtractor.DEFAULT_SESSION_TIMEOUT_MS).minusMillis(1)
                .toEpochMilli());

        HttpServletRequest request = Mockito.mock(HttpServletRequest.class);
        Mockito.when(request.getHeader("Authorization")).thenReturn("sso-jwt " + jwt.serialize());

        SsoToken token = ssoTokenExtractor.extractToken(request);
        assertNull(token);
    }

    @Test
    public void testCookieExpiredToken() throws Exception {

        // create an expired JWT token
        JWT jwt = newJWT(Instant.now().minusMillis(SsoTokenExtractor.DEFAULT_SESSION_TIMEOUT_MS).minusMillis(1)
                .toEpochMilli());

        HttpServletRequest request = Mockito.mock(HttpServletRequest.class);
        Cookie[] cookies = { new Cookie("auth_idp", jwt.serialize()) };
        Mockito.when(request.getCookies()).thenReturn(cookies);

        SsoToken token = ssoTokenExtractor.extractToken(request);
        assertNull(token);
    }
}
