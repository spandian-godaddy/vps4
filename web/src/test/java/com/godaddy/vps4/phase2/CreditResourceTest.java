package com.godaddy.vps4.phase2;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import javax.ws.rs.NotFoundException;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import com.godaddy.vps4.credit.CreditService;
import com.godaddy.vps4.credit.VirtualMachineCredit;
import com.godaddy.vps4.security.GDUserMock;
import com.godaddy.vps4.vm.AccountStatus;
import com.godaddy.vps4.web.Vps4Exception;
import com.godaddy.vps4.web.Vps4NoShopperException;
import com.godaddy.vps4.web.credit.CreditResource;
import com.godaddy.vps4.web.credit.CreditResource.CreateCreditRequest;
import com.godaddy.vps4.web.security.AdminOnly;
import com.godaddy.vps4.web.security.GDUser;
import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Provides;

public class CreditResourceTest {

    private GDUser user;
    private UUID orionGuid = UUID.randomUUID();
    private VirtualMachineCredit vmCredit;
    CreditService creditService = mock(CreditService.class);


    private Injector injector = Guice.createInjector(
            new AbstractModule() {

                @Override
                public void configure() {
                    bind(CreditService.class).toInstance(creditService);
                }

                @Provides
                public GDUser provideUser() {
                    return user;
                }
            });

    private CreditResource getCreditResource() {
        return injector.getInstance(CreditResource.class);
    }

    private VirtualMachineCredit createVmCredit(AccountStatus accountStatus) {
        return new VirtualMachineCredit(orionGuid, 10, 1, 0, "linux", "cPanel", null, user.getShopperId(), accountStatus, null, null,
                false);
    }

    @Before
    public void setupTest() {
        user = GDUserMock.createShopper();
        vmCredit = createVmCredit(AccountStatus.ACTIVE);
        when(creditService.getVirtualMachineCredit(orionGuid)).thenReturn(vmCredit);
        when(creditService.getUnclaimedVirtualMachineCredits("validUserShopperId"))
                     .thenReturn(Arrays.asList(vmCredit));
    }

    @Test
    public void testShopperGetCredit() {
        VirtualMachineCredit credit = getCreditResource().getCredit(orionGuid);
        Assert.assertEquals(orionGuid, credit.orionGuid);
    }

    @Test
    public void testEmployeeGetCredit() {
        user = GDUserMock.createEmployee();
        VirtualMachineCredit credit = getCreditResource().getCredit(orionGuid);
        Assert.assertEquals(orionGuid, credit.orionGuid);

    }

    @Test
    public void testAdminGetCredit() {
        user = GDUserMock.createAdmin();
        VirtualMachineCredit credit = getCreditResource().getCredit(orionGuid);
        Assert.assertEquals(orionGuid, credit.orionGuid);
    }

    @Test(expected=NotFoundException.class)
    public void testGetUnauthorizedCredit() {
        user = GDUserMock.createShopper("otherShopperId");
        getCreditResource().getCredit(orionGuid);
    }

    @Test(expected=NotFoundException.class)
    public void testNoCreditGetCredit() {
        UUID noSuchCreditGuid = UUID.randomUUID();
        getCreditResource().getCredit(noSuchCreditGuid);
    }

    @Test
    public void testShopperGetCredits() {
        List<VirtualMachineCredit> credits = getCreditResource().getCredits(false);
        Assert.assertTrue(credits.contains(vmCredit));
    }

    @Test
    public void testOtherShopperGetCredits() {
        user = GDUserMock.createShopper("otherShopperId");
        List<VirtualMachineCredit> credits = getCreditResource().getCredits(false);
        Assert.assertTrue(credits.isEmpty());
    }

    @Test
    public void testGetSuspendedCredits() {
        VirtualMachineCredit suspendedCredit = createVmCredit(AccountStatus.SUSPENDED);
        when(creditService.getVirtualMachineCredits(GDUserMock.DEFAULT_SHOPPER))
                .thenReturn(Arrays.asList(suspendedCredit));
        List<VirtualMachineCredit> credits = getCreditResource().getCredits(true);
        Assert.assertTrue(credits.contains(suspendedCredit));
    }

    @Test(expected=Vps4NoShopperException.class)
    public void testEmployeeGetCredits() {
        user = GDUserMock.createEmployee();
        // Employee user has no shopperid
        getCreditResource().getCredits(false);
    }

    @Test
    public void testE2SGetCredits() {
        user = GDUserMock.createEmployee2Shopper();
        List<VirtualMachineCredit> credits = getCreditResource().getCredits(false);
        Assert.assertTrue(credits.contains(vmCredit));
    }

    @Test
    public void testCreateCreditAdminOnly() {
        try {
            Method method = CreditResource.class.getMethod("createCredit", CreateCreditRequest.class);
            Assert.assertTrue(method.isAnnotationPresent(AdminOnly.class));
        }
        catch(NoSuchMethodException ex) {
            Assert.fail();
        }
    }
    @Test
    public void testCreateCredit() {
        CreateCreditRequest req = new CreateCreditRequest();
        req.operatingSystem = "Linux";
        req.controlPanel = "MYH";
        req.tier = 10;
        req.managedLevel = 0;
        req.monitoring = 0;
        req.shopperId = "someShopperId";

        user = GDUserMock.createAdmin(null);
        VirtualMachineCredit newCredit = getCreditResource().createCredit(req);

        verify(creditService).createVirtualMachineCredit(
                any(UUID.class), eq(req.operatingSystem), eq(req.controlPanel),
                eq(req.tier), eq(req.managedLevel), eq(req.monitoring), eq(req.shopperId));
        verify(creditService).getVirtualMachineCredit(any(UUID.class));

        Assert.assertEquals(
                creditService.getVirtualMachineCredit(any(UUID.class)), newCredit);
    }

    @Test
    public void testReleaseCreditAdminOnly() {
        try {
            Method method = CreditResource.class.getMethod("releaseCredit", UUID.class);
            Assert.assertTrue(method.isAnnotationPresent(AdminOnly.class));
        }
        catch(NoSuchMethodException ex) {
            Assert.fail();
        }
    }

    @Test
    public void testReleaseCredit() {
        user = GDUserMock.createAdmin(null);
        UUID creditGuid = UUID.randomUUID();
        VirtualMachineCredit freeCredit = getCreditResource().releaseCredit(creditGuid);

        verify(creditService).unclaimVirtualMachineCredit(creditGuid);
        verify(creditService).getVirtualMachineCredit(creditGuid);

        Assert.assertEquals(
                creditService.getVirtualMachineCredit(creditGuid), freeCredit);
    }

}
