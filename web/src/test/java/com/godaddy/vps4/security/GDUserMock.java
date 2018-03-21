package com.godaddy.vps4.security;

import static org.mockito.Mockito.when;

import com.godaddy.vps4.web.security.GDUser;
import org.mockito.Mockito;

public class GDUserMock {
    public final static String DEFAULT_SHOPPER = "validUserShopperId";

    public static GDUser createShopper() {
        return createShopper(DEFAULT_SHOPPER);
    }

    public static GDUser createShopper(String shopperId) {
        return create(shopperId, false, false);
    }

    public static GDUser createEmployee() {
        return create(null, true, false);
    }

    public static GDUser createEmployee2Shopper() {
        return createEmployee2Shopper(DEFAULT_SHOPPER);
    }

    public static GDUser createEmployee2Shopper(String shopperId) {
        return create(shopperId, true, false);
    }

    public static GDUser createAdmin(){
        return createAdmin(null);
    }

    public static GDUser createAdmin(String shopperId) {
        return create(shopperId, true, true);
    }

    public static GDUser create(String shopperId, boolean isEmployee, boolean isAdmin) {
        GDUser gdUser = Mockito.mock(GDUser.class);
        when(gdUser.isShopper()).thenReturn(shopperId!=null);
        when(gdUser.isEmployee()).thenReturn(isEmployee);
        when(gdUser.isAdmin()).thenReturn(isAdmin);
        when(gdUser.getShopperId()).thenReturn(shopperId);
        when(gdUser.isEmployeeToShopper()).thenReturn(isEmployee && shopperId != null);
        return gdUser;
    }
}
