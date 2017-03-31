package com.godaddy.vps4.web.security;

import java.util.List;

import javax.inject.Inject;
import javax.servlet.http.HttpServletRequest;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.godaddy.hfs.sso.token.JomaxSsoToken;
import com.godaddy.hfs.sso.token.SsoToken;
import com.godaddy.vps4.sso.SsoSupportTokenExtractor;

public class Vps4SupportRequestAuthenticator implements RequestAuthenticator<Boolean> {

    private final Logger logger = LoggerFactory.getLogger(Vps4SupportRequestAuthenticator.class);

    public static final String USER_ATTRIBUTE_NAME = "hfs-user";

    private final SsoSupportTokenExtractor tokenExtractor;

    private static final String REALM = "jomax";

    private static final String AUTH_TYPE_EMPOLYEE_TO_SHOPPER = "e2s";

    private static final String AUTH_TYPE_EMPLOYEE_TO_SHOPPER_TO_SHOPPER = "e2s2s";

    // TODO: change this group to be more configurable / granular (roles based on levels). 
    private static final String SEARCH_GROUP = "Development";

    @Inject
    public Vps4SupportRequestAuthenticator(SsoSupportTokenExtractor tokenExtractor) {
        this.tokenExtractor = tokenExtractor;
    }

    /**
     * Check if the user is a support administrator based on the token.
     * If the user is an employee with basic auth and jomax realm 
     * OR the user is employee delegate (e2s or e2s2s delegated auth_idp token in header or cookie)
     * OR if the user is authenticated and has an auth_jomax cookie
     */
    public Boolean authenticate(HttpServletRequest request) {
        logger.info("Authenticating user as a support administrator...");

        SsoToken token = tokenExtractor.extractToken(request);
        logger.info("Verifying support admin status: Token details: {}", token);

        if (token != null) {
            
            if(StringUtils.equalsIgnoreCase(REALM, token.getRealm())
                    || StringUtils.equals(AUTH_TYPE_EMPOLYEE_TO_SHOPPER, token.getAuthType())
                    || StringUtils.equals(AUTH_TYPE_EMPLOYEE_TO_SHOPPER_TO_SHOPPER, token.getAuthType()) ) {
                // TODO : Make changes to HFS code to add the groups to the token when its provided in the header.
                // And then check to make sure the employee belongs to the required group.
                // For now, its good enough to know its an employee with auth_jomax token.
                return true;
            }

        } 

        return this.authenticateJomaxCookie(request);
    }

    public boolean authenticateJomaxCookie(HttpServletRequest request) {

        SsoToken token = tokenExtractor.extractJomaxCookieToken(request.getCookies());
        logger.info("Checking group : Token details: {}", token);

        return authenticateGroup(token, SEARCH_GROUP);
    }

    public boolean authenticateGroup(SsoToken token, String groupToSearch) {
        if (token instanceof JomaxSsoToken) {
            List<String> groups = ((JomaxSsoToken) token).getGroups();
            if (groups != null && groups.size() > 0) {
                long count = groups.stream().filter(group -> group.equals(groupToSearch)).count();
                if (count > 0) {
                    return true;
                }
            }
        }

        logger.info("Could not verify user token belonging to support admin group.");
        return false;
    }
}
