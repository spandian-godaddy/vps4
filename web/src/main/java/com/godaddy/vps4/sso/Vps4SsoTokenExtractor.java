package com.godaddy.vps4.sso;

import java.util.Arrays;
import java.util.Date;
import java.util.List;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;

import com.nimbusds.jwt.ReadOnlyJWTClaimsSet;
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
    private final long sessionTimeoutMs;

    public Vps4SsoTokenExtractor(SsoService ssoService, long sessionTimeoutMs) {
        super(ssoService, sessionTimeoutMs);
        this.sessionTimeoutMs = sessionTimeoutMs;
    }

    @Override
    public void validate(SsoToken token) throws VerificationException, TokenExpiredException {
        if (!this.verify(token.jwt)) {
            throw new VerificationException("Unable to validate token");
        } else if (isExpired(token.claims, sessionTimeoutMs)) {
            throw new TokenExpiredException("Token has expired");
        }
    }

    protected static boolean isExpired(ReadOnlyJWTClaimsSet claims, long sessionTimeoutMs) {
        Date issueTime = claims.getIssueTime();
        if (issueTime == null) {
            throw new IllegalArgumentException("JWT does not have an 'issue time' field");
        }

        long issued = claims.getIssueTime().getTime();

        long now = System.currentTimeMillis();

        return now > (issued + sessionTimeoutMs);
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
        }

        return null;
    }

}
