package com.godaddy.vps4.sso;

import java.util.List;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.godaddy.hfs.sso.SsoService;
import com.godaddy.hfs.sso.SsoTokenExtractor;
import com.godaddy.hfs.sso.TokenExpiredException;
import com.godaddy.hfs.sso.VerificationException;
import com.godaddy.hfs.sso.token.IdpSsoToken;
import com.godaddy.hfs.sso.token.SsoToken;

public class Vps4SsoTokenExtractor extends SsoTokenExtractor {

    private static final Logger logger = LoggerFactory.getLogger(Vps4SsoTokenExtractor.class);

    public Vps4SsoTokenExtractor(SsoService ssoService, long sessionTimeoutMs) {
        super(ssoService, sessionTimeoutMs);
    }

    /*
     * Extract first valid sso token from the http request in the following priority order:
     *  1 - Authorization http header
     *  2 - auth_idp cookie
     *  3 - auth_jomax cookie
     *
     * @see com.godaddy.hfs.sso.SsoTokenExtractor#extractToken(javax.servlet.http.HttpServletRequest)
     */
    @Override
    public SsoToken extractToken(HttpServletRequest request) {
        // look for 'Authorization: sso-jwt [TOKEN]' header
        SsoToken token = extractAuthorizationHeaderToken(request);

        if (token == null) {
            // if the 'Authorization' header is not present (or the token could not be parsed),
            // fall back to the cookies with auth_idp preferred over auth_jomax
            token = extractCookieTokenWithIdpPriority(request.getCookies());
        }

        return token;
    }

    public SsoToken extractCookieTokenWithIdpPriority(Cookie[] cookies) {
        List<SsoToken> tokens = extractTokens(cookies);
        SsoToken validToken = null;
        for (SsoToken token : tokens) {
            try {
                validate(token);
                if (token instanceof IdpSsoToken)
                    return token;
                if (validToken == null)
                    validToken = token;
            }
            catch (VerificationException e) {
                logger.warn("Unable to verify token", e);
            }
            catch (TokenExpiredException e) {
                logger.warn("Token has expired", e);
            }
        }

        return validToken;
    }

}
