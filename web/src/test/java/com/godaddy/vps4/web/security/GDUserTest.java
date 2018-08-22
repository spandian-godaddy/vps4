package com.godaddy.vps4.web.security;

import org.junit.Test;

import com.godaddy.vps4.web.security.GDUser.Role;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertFalse;

public class GDUserTest {
    @Test
    public void returnsTrueWhenRoleMatches() throws Exception {
        GDUser gdUser = new GDUser();
        gdUser.role = Role.ADMIN;
        Role[] roles = {Role.ADMIN, Role.EMPLOYEE_OTHER, Role.HS_AGENT};
        assertTrue(gdUser.anyRole(roles));
    }

    @Test
    public void returnsFalseWhenRoleDoesntMatch() throws Exception {
        GDUser gdUser = new GDUser();
        gdUser.role = Role.CUSTOMER;
        Role[] roles = {Role.ADMIN, Role.EMPLOYEE_OTHER, Role.HS_AGENT};
        assertFalse(gdUser.anyRole(roles));
    }
}