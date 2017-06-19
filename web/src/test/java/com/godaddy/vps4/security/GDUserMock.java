package com.godaddy.vps4.security;

import org.mockito.Mockito;
import static org.mockito.Mockito.when;

import com.godaddy.vps4.web.security.GDUser;

public class GDUserMock {

    public static GDUser createShopper(String shopperId) {
        return create(shopperId, false, false);
    }

    public static GDUser createEmployee() {
        return create(null, true, false);
    }

    public static GDUser createEmployee2Shopper(String shopperId) {
        return create(shopperId, true, false);
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
        return gdUser;
    }
}
