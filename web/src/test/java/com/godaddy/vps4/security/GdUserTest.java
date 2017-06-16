package com.godaddy.vps4.security;

import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.List;

import org.junit.Test;
import org.mockito.Mockito;

import com.godaddy.hfs.sso.token.IdpSsoToken;
import com.godaddy.hfs.sso.token.JomaxSsoToken;
import com.godaddy.hfs.sso.token.SsoToken;
import com.godaddy.vps4.web.security.GDUser;

import junit.framework.Assert;

public class GdUserTest {

    public SsoToken mockIdpToken(String shopperId) {
        IdpSsoToken token = Mockito.mock(IdpSsoToken.class);
        when(token.getShopperId()).thenReturn(shopperId);
        return token;
    }

    public SsoToken mockJomaxToken(List<String> groups) {
        JomaxSsoToken token = Mockito.mock(JomaxSsoToken.class);
        when(token.getGroups()).thenReturn(groups);
        return token;
    }

    @Test
    public void testShopper() {
        SsoToken token = mockIdpToken("abc");
        GDUser user = new GDUser(token);
        Assert.assertEquals("abc", user.getShopperId());
        Assert.assertEquals(false, user.isStaff());
        Assert.assertEquals(false, user.isInternal());
    }

    @Test
    public void testShopperCannotOverride() {
        SsoToken token = mockIdpToken("abc");
        GDUser user = new GDUser(token, "xyz");
        Assert.assertEquals("abc", user.getShopperId());
        Assert.assertEquals(false, user.isStaff());
        Assert.assertEquals(false, user.isInternal());
    }

    @Test
    public void testAdmin() {
        SsoToken token = mockJomaxToken(Arrays.asList("Dev-VPS4"));
        GDUser user = new GDUser(token);
        Assert.assertEquals(null, user.getShopperId());
        Assert.assertEquals(true, user.isStaff());
        Assert.assertEquals(true, user.isInternal());
    }

    @Test
    public void testAdminWithShopperOverride() {
        SsoToken token = mockJomaxToken(Arrays.asList("Dev-VPS4"));
        GDUser user = new GDUser(token, "abc");
        Assert.assertEquals("abc", user.getShopperId());
        Assert.assertEquals(true, user.isStaff());
        Assert.assertEquals(true, user.isInternal());
    }

    @Test
    public void testEmployeeNotVps4() {
        SsoToken token = mockJomaxToken(Arrays.asList("Development"));
        GDUser user = new GDUser(token, "abc");
        Assert.assertEquals("abc", user.getShopperId());
        Assert.assertEquals(false, user.isStaff());
        Assert.assertEquals(true,  user.isInternal());
    }
}
