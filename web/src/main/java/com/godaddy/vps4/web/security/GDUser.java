package com.godaddy.vps4.web.security;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import com.godaddy.hfs.sso.token.SsoToken;

public class GDUser {
    // Role that this user is assigned in the Vps4 app
    public enum Role {
        ADMIN, CUSTOMER, EMPLOYEE_OTHER, HS_AGENT, HS_LEAD, SUSPEND_AUTH, C3_OTHER, MIGRATION, VPS4_API_READONLY
    }

    SsoToken token;
    String shopperId;
    boolean isEmployee;
    boolean isAdmin;
    String username;
    List<Role> roles = Collections.singletonList(Role.CUSTOMER); // default

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

    public List<Role> roles() {
        return roles;
    }

    public boolean anyRole(Role[] givenRoles) {
        return Arrays.stream(givenRoles).anyMatch(r -> this.roles.contains(r));
    }

    public boolean isCustomer() {
        return this.roles.contains(Role.CUSTOMER);
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
                + isEmployee + ", isAdmin=" + isAdmin + ", " +
                "isShopper()=" + isShopper() + ", username=" + username + ", role=" + roles.toString() + "]";
    }

}
