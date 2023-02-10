package com.godaddy.hfs.sso;

import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.godaddy.hfs.sso.token.SsoToken;
import com.godaddy.hfs.sso.token.SsoTokenReader;
import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.crypto.RSASSAVerifier;
import com.nimbusds.jose.jwk.JWK;
import com.nimbusds.jose.jwk.KeyType;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jwt.ReadOnlyJWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;

public class SsoTokenExtractor {

    public static final long DEFAULT_SESSION_TIMEOUT_MS = 2 * 60 * 60 * 1000;
    private static final String AUTH_HEADER_PREFIX = "sso-jwt ";
    public static final long DEFAULT_VERIFIED_TIMEOUT_MS = 10 * 60 * 1000;

    private static final Logger logger = LoggerFactory.getLogger(SsoTokenExtractor.class);

    private final SsoService ssoService;

    private final SsoTokenReader tokenReader;

    private final long sessionTimeoutMs;

    public SsoTokenExtractor(SsoService ssoService) {
        this(ssoService, DEFAULT_SESSION_TIMEOUT_MS);
    }

    public SsoTokenExtractor(SsoService ssoService, long sessionTimeoutMs) {
        this.ssoService = ssoService;
        this.sessionTimeoutMs = sessionTimeoutMs;
        this.tokenReader = new SsoTokenReader(ssoService);
    }

    public SsoToken extractToken(HttpServletRequest request) {

        // look for 'Authorization: sso-jwt [TOKEN]' header
        SsoToken token = extractAuthorizationHeaderToken(request);

        if (token == null) {
            // if the 'Authorization' header is not present (or the token could not be parsed),
            // fall back to the cookies
            token = extractFirstValidCookieToken(request.getCookies());
        }

        return token;
    }


    /**
     * extract 'Authorization: sso-jwt [TOKEN]'
     *
     * @param request
     * @return
     */
    public SsoToken extractAuthorizationHeaderToken(HttpServletRequest request) {
        String authHeader = request.getHeader("Authorization");
        if (authHeader != null) {
            // parse header, extract JWT
            if (authHeader.startsWith(AUTH_HEADER_PREFIX)) {
                String token = authHeader.substring(AUTH_HEADER_PREFIX.length());
                try {
                    SsoToken ssoToken = tokenReader.readSsoToken(token);
                    if (ssoToken != null) {
                        try {
                            validate(ssoToken);
                            if (ssoToken.delegateUser != null) {
                                validate(ssoToken.delegateUser);
                            }
                            if (ssoToken.employeeUser != null) {
                                validate(ssoToken.employeeUser);
                            }
                            if(ssoToken.certificateToken != null) {
                                validate(ssoToken.certificateToken);
                            }

                            return ssoToken;
                        }
                        catch (VerificationException e) {
                            logger.warn("Unable to verify token", e);
                        }
                        catch (TokenExpiredException e) {
                            logger.warn("Token has expired", e);
                        }
                    }

                }
                catch (ParseException e) {
                    logger.error("Unable to parse JWT token", e);
                }
            }
        }
        return null;
    }

    public SsoToken extractFirstValidCookieToken(Cookie[] cookies) {
        List<SsoToken> tokens = extractTokens(cookies);

        for (SsoToken token : tokens) {
            try {
                validate(token);
                return token;

            }
            catch (VerificationException e) {
                logger.warn("Unable to verify token", e);
            }
            catch (TokenExpiredException e) {
                logger.warn("Token has expired", e);
            }
        }

        logger.info("No valid tokens found");
        return null;
    }

    public List<SsoToken> extractTokens(Cookie[] cookies) {
        List<SsoToken> tokens = new ArrayList<>();
        if (cookies != null) {
            for (Cookie cookie : cookies) {
                try {
                    SsoToken token = tokenReader.readSsoToken(cookie);
                    if (token != null) {
                        tokens.add(token);
                    }
                }
                catch (ParseException e) {
                    logger.warn("Unable to parse SSO token", e);
                }
            }
        }
        return tokens;
    }

    public void validate(SsoToken token) throws VerificationException, TokenExpiredException {

        if (!verify(token.jwt)) {
            throw new VerificationException("Unable to validate token");
        }

        if (isExpired(token.claims, sessionTimeoutMs)) {
            throw new TokenExpiredException("Token has expired");
        }

    }

    protected boolean verify(SignedJWT signedToken, JWK key) throws VerificationException {

        if (!key.getKeyType().equals(KeyType.RSA)) {
            throw new VerificationException(
                    "Unsupported key format '" + key.getKeyType() + "' (only RSA keys accepted)");
        }

        RSAKey rsaKey = (RSAKey) key;

        try {
            RSASSAVerifier verifier = new RSASSAVerifier(rsaKey.toRSAPublicKey());

            return signedToken.verify(verifier);
        }
        catch (InvalidKeySpecException | JOSEException | NoSuchAlgorithmException e) {
            throw new VerificationException("Error while verifying key", e);
        }

    }

    protected boolean verify(SignedJWT jwt) throws VerificationException {
        // verify the signed key of the JWT against the key returned by the
        // SsoService
        String keyId = jwt.getHeader().getKeyID();

        JWK key = ssoService.getKey(keyId);

        if (key == null) {
            throw new VerificationException("Unknown key ID: " + keyId);
        }

        return verify(jwt, key);
    }

    protected static boolean isExpired(ReadOnlyJWTClaimsSet claims, long sessionTimeoutMs) {
        // This check assumes low business impact level of 1
        // When compared to auth-contrib libs
        long now = System.currentTimeMillis();

        // First check the validation last verified at time
        try {
            Long vat = claims.getLongClaim("vat");
            if (vat != null) {
                // vat time is in seconds, must convert to ms
                return now > ((vat * 1000) + DEFAULT_VERIFIED_TIMEOUT_MS);
            }
        } catch (ParseException e) {
            logger.error("Unable to parse 'vat' claim JWT token", e);
        }

        // If vat claim does not exist, verified issued time
        Date issueTime = claims.getIssueTime();
        if (issueTime == null) {
            throw new IllegalArgumentException("JWT does not have an 'issue time' field");
        }

        long issued = claims.getIssueTime().getTime();

        return now > (issued + sessionTimeoutMs);
    }

}
