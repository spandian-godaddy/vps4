package com.godaddy.vps4.web.security;


import java.util.Arrays;

import com.godaddy.hfs.sso.token.SsoToken;

public class GDUser {
    // Role that this user is assigned in the Vps4 app
    public enum Role {
        ADMIN, CUSTOMER, EMPLOYEE_OTHER, HS_AGENT, HS_LEAD, SUSPEND_AUTH
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

    public boolean is3LetterAccount() {
        return shopperId.length() == 3;
    }

    public boolean isPayingCustomer() {
        return isCustomer() && !is3LetterAccount();
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
