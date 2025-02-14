package com.godaddy.vps4.web.security;

import java.util.List;
import java.util.Arrays;
import java.util.Objects;
import java.util.ArrayList;

import javax.inject.Inject;
import javax.servlet.http.HttpServletRequest;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.godaddy.hfs.config.Config;
import com.godaddy.hfs.sso.SsoTokenExtractor;
import com.godaddy.hfs.sso.token.CertificateToken;
import com.godaddy.hfs.sso.token.IdpSsoToken;
import com.godaddy.hfs.sso.token.JomaxSsoToken;
import com.godaddy.hfs.sso.token.SsoToken;
import com.godaddy.vps4.web.Vps4Exception;
import com.godaddy.vps4.web.security.GDUser.Role;


public class SsoRequestAuthenticator implements RequestAuthenticator<GDUser> {

    private static final Logger logger = LoggerFactory.getLogger(SsoRequestAuthenticator.class);

    private final String VPS4_TEAM = "Dev-VPS4";
    private final String C3_HOSTING_SUPPORT = "C3-Hosting Support";
    private final String CA_GAO_HTL = "C3-GAO-HTL";
    private final String ORG_TECH_SERVICE_SYSADMIN= "org-technical-services-sysadmins";
    private final String LEGAL = "fs-Legal_IP_Claims";
    private final String HOSTING_OPERATIONS = "Hosting Ops";
    private final String DIGITAL_CRIMES_UNIT = "DCU-Phishstory";
    private final String CHARGEBACK = "Chargeback User";
    private final String DEV_PTGS = "Dev-PTGS";
    private final String CSR = "CSR";
    private final String MEDIA_TEMPLE_CS = "Media Temple - CS";
    private final String MIGRATION_TOOL = "Migration-Engine-SG";
    private final String CSM = "CSM";
    private final String DEV_VERTIGO = "Dev-Vertigo";
    private final String VPS4_API_READONLY = "VPS4-API-ReadOnly";
    private final String C3_SUCURI_SUPPORT = "C3-Sucuri-Support";
    private final String VZ_CLOUD = "VZ-CLOUD";

    private final SsoTokenExtractor tokenExtractor;
    private final Config config;

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

        // deny users API access to inactive data center unless they are a 3 letter shopper id.
        if(denyAccessToInactiveDc(gdUser)) {
            logger.info("User {} does not have an account with 3 letter shopper id. Denying access to API in INACTIVE Data Center. ", gdUser.toString());
            return null;
        }

        return gdUser;
    }

    /**
     * deny API access to inactive data center for all gd users unless they are a 3 letter account.
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
            gdUser.username = ((JomaxSsoToken) token).getUsername();
            gdUser.shopperId = shopperOverride;
            gdUser.isEmployee = true;
            gdUser.isShopperInjected = shopperOverride != null;
            setPrivilegeByGroups(gdUser, ((JomaxSsoToken) token).getGroups());
        }
        else if (token instanceof IdpSsoToken) {
            gdUser.shopperId = ((IdpSsoToken) token).getShopperId();
            if (token.employeeUser != null) {
                gdUser.isEmployee = true;
                setPrivilegeByGroups(gdUser, ((JomaxSsoToken) token.employeeUser).getGroups());
                gdUser.username = ((JomaxSsoToken) token.employeeUser).getUsername();
            } else {
                gdUser.username = "Customer";
            }
        }
        else if (token instanceof CertificateToken) {
            if (!isAllowedCertCN(gdUser, ((CertificateToken) token).cn)) {
                String errorMsg = "Certificate not allowed: " + ((CertificateToken) token).cn;
                logger.error(errorMsg);
                throw new Vps4Exception("AUTHORIZATION_ERROR", errorMsg);
            }
        }
        else
            throw new Vps4Exception("AUTHORIZATION_ERROR", "Unknown SSO Token Type!");

        return gdUser;
    }

    private void setPrivilegeByGroups(GDUser gdUser, List<String> groups) {
        List<Role> userRoles = new ArrayList<>();
        if (groups.contains(VPS4_TEAM) ||
            groups.contains(DEV_PTGS) ||
            groups.contains(DEV_VERTIGO)) {
            gdUser.isAdmin = true;
            userRoles.add(Role.ADMIN);
        } if (groups.contains(CA_GAO_HTL) ||
              groups.contains(ORG_TECH_SERVICE_SYSADMIN)) {
            userRoles.add(Role.HS_LEAD);
        } if (groups.contains(HOSTING_OPERATIONS) ||
                groups.contains(LEGAL) ||
                groups.contains(DIGITAL_CRIMES_UNIT) ||
                groups.contains(CHARGEBACK) ||
                groups.contains(CSM)) {
            userRoles.add(Role.SUSPEND_AUTH);
        } if (groups.contains(C3_HOSTING_SUPPORT) ||
                groups.contains(MEDIA_TEMPLE_CS) ||
                groups.contains(C3_SUCURI_SUPPORT)) {
            userRoles.add(Role.HS_AGENT);
        } if (groups.contains(CSR)) {
            userRoles.add(Role.C3_OTHER);
        } if (groups.contains(MIGRATION_TOOL) ||
                groups.contains(VZ_CLOUD)) {
            userRoles.add(Role.IMPORT);
        } if (groups.contains(VPS4_API_READONLY)) {
            userRoles.add(Role.VPS4_API_READONLY);
        } if (userRoles.isEmpty()) {
            userRoles.add(Role.EMPLOYEE_OTHER);
        } if (gdUser.isShopper()) {
            userRoles.add(Role.CUSTOMER); // for employee impersonation
        }
        gdUser.roles = userRoles;
    }

    private boolean isAllowedCertCN(GDUser gdUser, String certCn) {
        String authzCertsConfig = config.get("authz_certs");

        try {
            ObjectMapper mapper = new ObjectMapper();
            List<AuthzSsoCert> authzCerts = new ArrayList<>(Arrays.asList(mapper.readValue(authzCertsConfig,
                                                                                           AuthzSsoCert[].class)));
            
            AuthzSsoCert ssoCertConfig = authzCerts.stream()
                .filter(c -> c.cn.equals(certCn))
                .findAny()
                .orElse(null);

            if (!Objects.isNull(ssoCertConfig)) {
                gdUser.username = ssoCertConfig.name;
                List<Role> userRoles = new ArrayList<>();
                userRoles.add(Role.valueOf(ssoCertConfig.role));
                gdUser.username = ssoCertConfig.name;
                gdUser.roles = userRoles;
                gdUser.isEmployee = false;
                return true;
            }
            return false;
        } catch (Exception ex) {
            String errorMsg = "Error reading config file.";
            throw new Vps4Exception("AUTHORIZATION_ERROR", errorMsg);
        }
    }
}
