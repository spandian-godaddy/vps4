package com.godaddy.vps4.sso;

import java.util.Arrays;
import java.util.List;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.godaddy.hfs.sso.SsoService;
import com.godaddy.hfs.sso.SsoTokenExtractor;
import com.godaddy.hfs.sso.TokenExpiredException;
import com.godaddy.hfs.sso.VerificationException;
import com.godaddy.hfs.sso.TokenInvalidException;
import com.godaddy.hfs.sso.token.IdpSsoToken;
import com.godaddy.hfs.sso.token.SsoToken;
import com.godaddy.hfs.sso.token.SsoTokenReader;

public class Vps4SsoTokenExtractor extends SsoTokenExtractor {
    private static final Logger logger = LoggerFactory.getLogger(Vps4SsoTokenExtractor.class);

    public Vps4SsoTokenExtractor(SsoService ssoService) {
        super(ssoService, SsoTokenReader.BUSINESS_IMPACT_VALIDITY_LEVEL_MEDIUM);
    }

    /*
     * Extract first valid sso token from the http request in the following priority order:
     *  1 - Authorization http header
     *  2 - auth_idp cookie (the auth_jomax cookie is not accepted)
     *
     * @see com.godaddy.hfs.sso.SsoTokenExtractor#extractToken(javax.servlet.http.HttpServletRequest)
     */
    @Override
    public SsoToken extractToken(HttpServletRequest request) {
        // look for 'Authorization: sso-jwt [TOKEN]' header
        SsoToken token = extractAuthorizationHeaderToken(request);

        if (token == null) {
            // if the 'Authorization' header is not present (or the token could not be parsed),
            // fall back to the auth_idp cookie (the auth_jomax cookie is not accepted)
            token = request.getCookies() == null ? null : extractIdpCookie(request.getCookies());
        }

        return token;
    }

    public SsoToken extractIdpCookie(Cookie[] cookies) {
            cookies = Arrays.stream(cookies).filter(c ->
                            c.getName() != null && c.getName().equals("auth_idp"))
                    .toArray(Cookie[]::new);

        List<SsoToken> tokens = extractTokens(cookies);
        for (SsoToken token : tokens) {
            try {
                if (token instanceof IdpSsoToken) {
                    validate(token);
                    return token;
                }
            }
            catch (VerificationException e) {
                logger.warn("Unable to verify token", e);
            }
            catch (TokenExpiredException e) {
                logger.warn("Token has expired", e);
            }
           catch (TokenInvalidException e) {
                logger.warn("Token is invalid", e);
           }
        }

        return null;
    }

}
