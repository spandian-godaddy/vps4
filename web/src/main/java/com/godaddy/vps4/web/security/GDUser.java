package com.godaddy.vps4.web.security;

import java.util.List;

import com.godaddy.hfs.sso.token.SsoToken;

public class GDUser {

    private final static String VPS4_TEAM = "Dev-VPS4";
    SsoToken token;
    String shopperId;
    List<String> employeeGroups;
    String username;

    public String getShopperId() {
        return shopperId;
    }

    public boolean isShopper() {
        return shopperId != null;
    }

    public boolean isEmployee() {
        return employeeGroups != null;
    }

    public boolean isAdmin() {
        return isEmployee() && employeeGroups.contains(VPS4_TEAM);
    }

    @Override
    public String toString() {
        return "GDUser [token=" + token + ", shopperId=" + shopperId + ", employeeGroups=" + employeeGroups
                + ", username=" + username + ", isShopper()=" + isShopper() + ", isEmployee()=" + isEmployee()
                + ", isAdmin()=" + isAdmin() + "]";
    }

}
