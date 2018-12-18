package com.godaddy.vps4.web.security;


import com.godaddy.hfs.sso.token.SsoToken;

import java.util.Arrays;

public class GDUser {
    // Role that this user is assigned in the Vps4 app
    public enum Role {
        ADMIN, CUSTOMER, EMPLOYEE_OTHER, HS_AGENT, HS_LEAD, LEGAL, HS_OPS
    };

    SsoToken token;
    String shopperId;
    boolean isEmployee;
    boolean isStaff;
    boolean isAdmin;
    String username;
    Role role = Role.CUSTOMER; // default

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
        return isStaff ;
    }

    public Role role() {
        return role;
    }

    public boolean anyRole(Role[] roles) {
        return Arrays.stream(roles).anyMatch(r -> r.equals(this.role));
    }

    public boolean isCustomer() {
        return Role.CUSTOMER.equals(this.role);
    }

    public boolean isEmployeeToShopper() {
        return isEmployee() && isShopper();
    }

    public String getUsername() {
        return username;
    }

    @Override
    public String toString() {
        return "GDUser [token=" + token + ", shopperId=" + shopperId + ", isEmployee="
                + isEmployee + ", isStaff=" + isStaff + ", isAdmin=" + isAdmin + "," +
                "isShopper()=" + isShopper() + ", username=" + username + ", role=" + role + "]";
    }

}
