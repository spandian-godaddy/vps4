package com.godaddy.vps4.web.security;


import com.godaddy.hfs.sso.token.SsoToken;

public class GDUser {

    SsoToken token;
    String shopperId;
    boolean isEmployee;
    boolean isStaff;
    boolean isAdmin;

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

    public boolean isStaff() {
        return isStaff;
    }

    public boolean isEmployeeToShopper() {
        return isEmployee() && isShopper();
    }

    @Override
    public String toString() {
        return "GDUser [token=" + token + ", shopperId=" + shopperId + ", isEmployee=" + isEmployee + ", isStaff="
                + isStaff + ", isAdmin=" + isAdmin + ", isShopper()=" + isShopper() + "]";
    }

}
