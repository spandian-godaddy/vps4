package com.godaddy.vps4.sso;

import javax.servlet.http.HttpServletRequest;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.godaddy.hfs.sso.SsoService;
import com.godaddy.hfs.sso.SsoTokenExtractor;
import com.godaddy.hfs.sso.token.SsoToken;

public class FallbackSsoTokenExtractor extends Vps4SsoTokenExtractor {

    private static final Logger logger = LoggerFactory.getLogger(FallbackSsoTokenExtractor.class);

    final SsoTokenExtractor fallbackExtractor;

    public FallbackSsoTokenExtractor(SsoService ssoService, long ssoTimeoutMs, SsoTokenExtractor fallbackExtractor) {
        super(ssoService, ssoTimeoutMs);
        this.fallbackExtractor = fallbackExtractor;
    }

    @Override
    public SsoToken extractToken(HttpServletRequest request) {
        try {
            // extractToken will try to authenticate via both auth header(1st) and cookie(2nd)
            // - reinstating cookie lookup as previously we were extracting jwt
            // - from auth header only and did not lookup auth_idp cookie for CSRF protection
            return super.extractToken(request);
        } catch (Exception ex) {
            // The OTE env currently shares the staging env as an example of using a fallback extractor
            logger.info("Staging Env: SSO key lookup failed, trying in OTE Environment");
            return fallbackExtractor.extractToken(request);
        }
    }

}