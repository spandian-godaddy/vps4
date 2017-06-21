package com.godaddy.vps4.web.security;

import javax.inject.Inject;
import javax.servlet.http.HttpServletRequest;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.godaddy.hfs.sso.SsoTokenExtractor;
import com.godaddy.hfs.sso.token.IdpSsoToken;
import com.godaddy.hfs.sso.token.JomaxSsoToken;
import com.godaddy.hfs.sso.token.SsoToken;
import com.godaddy.vps4.web.Vps4Exception;
import com.godaddy.vps4.web.util.AlphaHelper;

public class SsoRequestAuthenticator implements RequestAuthenticator<GDUser> {

    private static final Logger logger = LoggerFactory.getLogger(SsoRequestAuthenticator.class);

    private final String VPS4_TEAM = "Dev-VPS4";

    private final SsoTokenExtractor tokenExtractor;
    private final AlphaHelper alphaHelper;

    @Inject
    public SsoRequestAuthenticator(SsoTokenExtractor tokenExtractor,
            AlphaHelper alphaHelper) {
        this.tokenExtractor = tokenExtractor;
        this.alphaHelper = alphaHelper;
    }

    @Override
    public GDUser authenticate(HttpServletRequest request) {

        SsoToken token = tokenExtractor.extractToken(request);
        if (token == null) {
            return null;
        }

        GDUser gdUser = createGDUser(token, request);
        if (gdUser.isShopper()) {
            alphaHelper.verifyValidAlphaUser(gdUser.getShopperId());
        }

        logger.info("GD User authenticated: {}, URI: {}", gdUser.toString(), request.getRequestURI());

        return gdUser;
    }

    private GDUser createGDUser(SsoToken token, HttpServletRequest request) {
        String shopperOverride = request.getHeader("X-Shopper-Id");
        GDUser gdUser = new GDUser();
        gdUser.token = token;
        gdUser.username = token.getUsername();
        if (token instanceof JomaxSsoToken) {
            gdUser.shopperId = shopperOverride;
            gdUser.isEmployee = true;
            gdUser.isAdmin = ((JomaxSsoToken) token).getGroups().contains(VPS4_TEAM);
        }
        else if (token instanceof IdpSsoToken) {
            gdUser.shopperId = ((IdpSsoToken) token).getShopperId();
            if (token.employeeUser != null) {
                gdUser.isEmployee = true;
                gdUser.isAdmin = ((JomaxSsoToken) token.employeeUser)
                        .getGroups().contains(VPS4_TEAM);
            }
        }
        else
            throw new Vps4Exception("AUTHORIZATION_ERROR", "Unknown SSO Token Type!");

        return gdUser;
    }
}
