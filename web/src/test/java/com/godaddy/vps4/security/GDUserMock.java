package com.godaddy.vps4.security;

import static org.mockito.Mockito.when;

import org.mockito.Mockito;

import com.godaddy.vps4.web.security.GDUser;
import com.godaddy.vps4.web.security.GDUser.Role;

import java.util.Arrays;
import java.util.List;

public class GDUserMock {
    public final static String DEFAULT_SHOPPER = "validUserShopperId";

    public static GDUser createShopper() {
        return createShopper(DEFAULT_SHOPPER);
    }

    public static GDUser createShopper(String shopperId) {
        return create(shopperId, false, false, Arrays.asList(Role.CUSTOMER));
    }

    public static GDUser createEmployee() {
        return create(null, true, false, Arrays.asList(Role.EMPLOYEE_OTHER));
    }

    public static GDUser createStaff() {
        return create(null, true, false, Arrays.asList(Role.HS_AGENT));
    }

    public static GDUser createEmployee2Shopper() {
        return createEmployee2Shopper(DEFAULT_SHOPPER);
    }

    public static GDUser createEmployee2Shopper(String shopperId) {
        return create(shopperId, true, false, Arrays.asList(Role.EMPLOYEE_OTHER));
    }

    public static GDUser createAdmin() {
        return createAdmin(null);
    }

    public static GDUser createAdmin(String shopperId) {
        return create(shopperId, true, true, Arrays.asList(Role.ADMIN));
    }

    public static GDUser create(String shopperId, boolean isEmployee, boolean isAdmin, List<Role> roles) {
        GDUser gdUser = Mockito.mock(GDUser.class);
        when(gdUser.isShopper()).thenReturn(shopperId!=null);
        when(gdUser.isEmployee()).thenReturn(isEmployee);
        when(gdUser.isAdmin()).thenReturn(isAdmin);
        when(gdUser.getShopperId()).thenReturn(shopperId);
        when(gdUser.isEmployeeToShopper()).thenReturn(isEmployee && shopperId != null);
        when(gdUser.getUsername()).thenReturn("tester");
        when(gdUser.roles()).thenReturn(roles);
        return gdUser;
    }
}
