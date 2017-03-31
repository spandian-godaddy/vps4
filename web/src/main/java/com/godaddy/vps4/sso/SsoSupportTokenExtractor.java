package com.godaddy.vps4.sso;

import java.util.Arrays;
import java.util.List;

import javax.inject.Inject;
import javax.servlet.http.Cookie;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.godaddy.hfs.sso.KeyService;
import com.godaddy.hfs.sso.SsoTokenExtractor;
import com.godaddy.hfs.sso.TokenExpiredException;
import com.godaddy.hfs.sso.VerificationException;
import com.godaddy.hfs.sso.token.SsoToken;

public class SsoSupportTokenExtractor extends SsoTokenExtractor {

    private static final Logger logger = LoggerFactory.getLogger(SsoSupportTokenExtractor.class);
        
    private static final String TYPE_JOMAX = "auth_jomax";
    
    @Inject
    public SsoSupportTokenExtractor(KeyService keyService) {
        super(keyService);
    }
    
    public SsoToken extractJomaxCookieToken(Cookie[] cookies) {
        
        Cookie[] filteredCookies = Arrays.stream(cookies)
                .filter(cookie -> cookie.getName().equals(TYPE_JOMAX))
                .toArray(Cookie[]::new);
        
        List<SsoToken> tokens = extractTokens(filteredCookies);

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

}
