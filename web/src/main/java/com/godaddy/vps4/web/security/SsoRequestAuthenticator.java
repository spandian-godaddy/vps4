package com.godaddy.vps4.web.security;

import java.util.List;

import javax.inject.Inject;
import javax.servlet.http.HttpServletRequest;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.godaddy.hfs.config.Config;
import com.godaddy.hfs.sso.SsoTokenExtractor;
import com.godaddy.hfs.sso.token.IdpSsoToken;
import com.godaddy.hfs.sso.token.JomaxSsoToken;
import com.godaddy.hfs.sso.token.SsoToken;
import com.godaddy.vps4.web.Vps4Exception;

public class SsoRequestAuthenticator implements RequestAuthenticator<GDUser> {

    private static final Logger logger = LoggerFactory.getLogger(SsoRequestAuthenticator.class);

    private final String VPS4_TEAM = "Dev-VPS4";
    private final String C3_HOSTING_SUPPORT = "C3-Hosting Support";

    private final SsoTokenExtractor tokenExtractor;

    private Config config;

    @Inject
    public SsoRequestAuthenticator(SsoTokenExtractor tokenExtractor, Config config) {
        this.tokenExtractor = tokenExtractor;
        this.config = config;
    }

    @Override
    public GDUser authenticate(HttpServletRequest request) {

        SsoToken token = tokenExtractor.extractToken(request);
        if (token == null) {
            return null;
        }

        GDUser gdUser = createGDUser(token, request);
        logger.info("GD User authenticated: {}, URI: {}", gdUser.toString(), request.getRequestURI());

        // deny users API access to Inactive Datacenter unless they are a 3 letter shopper id.
        if(denyAccessToInactiveDc(gdUser)) {
            logger.info("User {} does not have an account with 3 letter shopper id. Denying access to API in INACTIVE Datacenter. ", gdUser.toString());
            return null;
        }

        return gdUser;
    }

    /**
     * deny API access to inactive Datacenter for all gd users unless they are a 3 letter account.
     * @param gdUser
     * @return true if DC is inactive and user is 3 letter account, false otherwise.
     */
    private boolean denyAccessToInactiveDc(GDUser gdUser) {
        try {
            return Boolean.parseBoolean(config.get("vps4.is.dc.inactive"))
                    && (!is3LetterAccount(gdUser.getShopperId()));
        } catch (Exception ex) {
            return false;
        }
    }

    /**
     * Return true if shopper id is 3 letter account.
     * @param shopperId
     * @return true if shopper id is 3 letter account
     */
    private boolean is3LetterAccount(String shopperId) {
        return shopperId.length() == 3;
    }

    private GDUser createGDUser(SsoToken token, HttpServletRequest request) {
        String shopperOverride = request.getHeader("X-Shopper-Id");
        GDUser gdUser = new GDUser();
        gdUser.token = token;
        if (token instanceof JomaxSsoToken) {
            gdUser.shopperId = shopperOverride;
            gdUser.isEmployee = true;
            setPrivilegeByGroups(gdUser, ((JomaxSsoToken) token).getGroups());
        }
        else if (token instanceof IdpSsoToken) {
            gdUser.shopperId = ((IdpSsoToken) token).getShopperId();
            if (token.employeeUser != null) {
                gdUser.isEmployee = true;
                setPrivilegeByGroups(gdUser, ((JomaxSsoToken) token.employeeUser).getGroups());
            }
        }
        else
            throw new Vps4Exception("AUTHORIZATION_ERROR", "Unknown SSO Token Type!");

        return gdUser;
    }

    private void setPrivilegeByGroups(GDUser gdUser, List<String> groups) {
        if (groups.contains(VPS4_TEAM)) {
            gdUser.isAdmin = true;
            gdUser.isStaff = true;
        } else if (groups.contains(C3_HOSTING_SUPPORT)) {
            gdUser.isStaff = true;
        }
    }
}
