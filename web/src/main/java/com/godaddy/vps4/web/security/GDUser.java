package com.godaddy.vps4.web.security;

import com.godaddy.hfs.sso.token.SsoToken;

public class GDUser {

    SsoToken token;
    String shopperId;
    boolean isEmployee;
    boolean isAdmin;
    String username;

    public String getShopperId() {
        return shopperId;
    }

    public boolean isShopper() {
        return shopperId != null;
    }

    public boolean isEmployee() {
        return isEmployee;
    }

    public boolean isAdmin() {
        return isAdmin;
    }

    @Override
    public String toString() {
        return "GDUser [token=" + token + ", shopperId=" + shopperId + ", username=" + username + ", isShopper()="
                + isShopper() + ", isEmployee()=" + isEmployee() + ", isAdmin()=" + isAdmin() + "]";
    }

}
