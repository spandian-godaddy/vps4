package com.godaddy.vps4.web.security;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.godaddy.hfs.sso.token.IdpSsoToken;
import com.godaddy.hfs.sso.token.JomaxSsoToken;
import com.godaddy.hfs.sso.token.SsoToken;

public class GDUser {

    private final static String VPS4_TEAM = "Dev-VPS4";
    private SsoToken token;
    private String shopperId;
    private boolean isStaff = false;
    private boolean isInternal = false;

    private static final Logger logger = LoggerFactory.getLogger(GDUser.class);

    public GDUser(SsoToken token) {
        this(token, null);
    }

    public GDUser(SsoToken token, String shopperOverride) {
        this.token = token;
        setShopperId(shopperOverride);
        setIsStaff();
        logger.error(this.toString());
    }

    private void setShopperId(String shopperOverride) {
        if (token instanceof IdpSsoToken)
            this.shopperId = ((IdpSsoToken) token).getShopperId();
        else if (token instanceof JomaxSsoToken)
            this.shopperId = shopperOverride;
    }

    private void setIsStaff() {
        if (token instanceof JomaxSsoToken) {
            this.isInternal = true;
            this.isStaff = ((JomaxSsoToken) token).getGroups().contains(VPS4_TEAM);
        }
    }

    public String getShopperId() {
        return shopperId;
    }

    public boolean isStaff() {
        return isStaff;
    }

    public boolean isInternal() {
        return isInternal;
    }

    @Override
    public String toString() {
        return "GDUser [shopperId=" + shopperId + ", isStaff=" + isStaff + ", isInternal="
                + isInternal + "]";
    }
}
