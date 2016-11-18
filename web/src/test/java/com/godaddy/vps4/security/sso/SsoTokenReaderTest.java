package com.godaddy.vps4.security.sso;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import javax.servlet.http.Cookie;

import org.junit.Test;

import com.godaddy.vps4.web.security.sso.token.IdpSsoToken;
import com.godaddy.vps4.web.security.sso.token.SsoToken;
import com.godaddy.vps4.web.security.sso.token.SsoTokenReader;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;

public class SsoTokenReaderTest {

    private static final String AUTH_IDP_VALUE = "eyJhbGciOiAiUlMyNTYiLCAia2lkIjogIkl0WTRlOXNOT3cifQ.eyJ1c2VybmFtZSI6ICI1eTUiLCAiZmlyc3RuYW1lIjogIkJyaWFuIiwgImZhY3RvcnMiOiB7ImtfcHciOiAxNDU0NjIwMzQ5fSwgImxhc3RuYW1lIjogIkRpZWtlbG1hbiIsICJhdXRoIjogImJhc2ljIiwgImZ0YyI6IDEsICJzaG9wcGVySWQiOiAiNXk1IiwgImp0aSI6ICI3YVFQelI2eHRxa0JvRFAzRGprUDR3IiwgInBsaWQiOiAiMSIsICJpYXQiOiAxNDU0NjIwMzQ5LCAidHlwIjogImlkcCJ9.aXW0_AYK3IdB0YRI9LDqL2N0hqM02zNf6JFdPElMllRxWls7b6QYgljgek3yRooYVZ2BOLLZbX8CEailbC5q5L3m6m-Yp9vtzvxzNqnPkPeCs4NUO-KzWu_60tIpfBeOvogWMtnxTBrTKDowQM6ahK1-Z-EFjsa_AS0_iZOeuCE";

    @Test
    public void testIdp() throws Exception {

        SsoTokenReader reader = new SsoTokenReader();

        Cookie cookie = new Cookie("auth_idp", AUTH_IDP_VALUE);

        SsoToken token = reader.readSsoToken(cookie);
        assertNotNull(token);

        assertEquals("Brian", token.getFirstName());
        assertEquals("Diekelman", token.getLastName());
    }

    @Test
    public void testFields() throws Exception {

        SsoTokenReader reader = new SsoTokenReader();

        JWTClaimsSet claims = new JWTClaimsSet();
        claims.setCustomClaim("firstname", "Brian");
        claims.setCustomClaim("lastname", "Diekelman");
        claims.setCustomClaim("username", "");
        claims.setCustomClaim("shopperId", "1234");
        claims.setCustomClaim("typ", "idp");
        claims.setCustomClaim("plid", "1");
        claims.setCustomClaim("auth", "basic");

        JWSHeader header = new JWSHeader(JWSAlgorithm.RS512);
        SignedJWT jwt = new SignedJWT(header, claims);

        {
            SsoToken token = reader.readSsoToken(jwt);
            assertNotNull(token);
            assertEquals("Brian", token.getFirstName());
            assertEquals("Diekelman", token.getLastName());
            assertEquals("1", ((IdpSsoToken) token).getPrivateLabelId());
        }

        // should be able to read string or integer 'plid' field
        {
            claims.setCustomClaim("plid", 1);

            SsoToken token = reader.readSsoToken(jwt);
            assertNotNull(token);
            assertEquals("1", ((IdpSsoToken) token).getPrivateLabelId());
        }

    }

}
