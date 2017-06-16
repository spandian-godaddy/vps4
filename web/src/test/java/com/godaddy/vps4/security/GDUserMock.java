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

    public static GDUser createAdmin(String shopperId) {
        return create(shopperId, true, true);
    }

    public static GDUser create(String shopperId, boolean isInternal, boolean isStaff) {
        GDUser gdUser = Mockito.mock(GDUser.class);
        when(gdUser.isStaff()).thenReturn(isStaff);
        when(gdUser.isInternal()).thenReturn(isInternal);
        when(gdUser.getShopperId()).thenReturn(shopperId);
        return gdUser;
    }
}
