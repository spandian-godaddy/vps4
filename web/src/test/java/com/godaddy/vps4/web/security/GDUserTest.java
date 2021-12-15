package com.godaddy.vps4.web.security;

import org.junit.Test;

import com.godaddy.vps4.web.security.GDUser.Role;

import java.util.Arrays;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertFalse;

public class GDUserTest {
    @Test
    public void returnsTrueWhenRoleMatches() throws Exception {
        GDUser gdUser = new GDUser();
        gdUser.roles = Arrays.asList(Role.ADMIN);
        Role[] roles = {Role.ADMIN, Role.EMPLOYEE_OTHER, Role.HS_AGENT};
        assertTrue(gdUser.anyRole(roles));
    }

    @Test
    public void returnsFalseWhenRoleDoesntMatch() throws Exception {
        GDUser gdUser = new GDUser();
        gdUser.roles = Arrays.asList(Role.CUSTOMER);
        Role[] roles = {Role.ADMIN, Role.EMPLOYEE_OTHER, Role.HS_AGENT};
        assertFalse(gdUser.anyRole(roles));
    }

    @Test
    public void isPayingCustomerReturnsTrue() throws Exception {
        GDUser gdUser = new GDUser();
        gdUser.roles = Arrays.asList(Role.CUSTOMER);
        gdUser.shopperId = "123435";
        assertTrue(gdUser.isPayingCustomer());
    }

    @Test
    public void isPayingCustomerReturnsFalseFor3LetterAccounts() throws Exception {
        GDUser gdUser = new GDUser();
        gdUser.roles = Arrays.asList(Role.CUSTOMER);
        gdUser.shopperId = "123";
        assertFalse(gdUser.isPayingCustomer());
    }

    @Test
    public void isPayingCustomerReturnsFalseForNonCustomer() throws Exception {
        GDUser gdUser = new GDUser();
        gdUser.roles = Arrays.asList(Role.ADMIN);
        assertFalse(gdUser.isPayingCustomer());
    }
}