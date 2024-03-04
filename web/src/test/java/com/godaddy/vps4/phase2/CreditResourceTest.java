package com.godaddy.vps4.phase2;

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.times;

import java.lang.reflect.Method;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import javax.ws.rs.NotFoundException;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import com.godaddy.hfs.mailrelay.MailRelay;
import com.godaddy.vps4.credit.CreditService;
import com.godaddy.vps4.credit.ECommCreditService.ProductMetaField;
import com.godaddy.vps4.credit.VirtualMachineCredit;
import com.godaddy.vps4.security.GDUserMock;
import com.godaddy.vps4.vm.VirtualMachineService;
import com.godaddy.vps4.vm.AccountStatus;
import com.godaddy.vps4.vm.DataCenterService;
import com.godaddy.vps4.web.Vps4NoShopperException;
import com.godaddy.vps4.web.credit.CreditResource;
import com.godaddy.vps4.web.credit.Vps4Credit;
import com.godaddy.vps4.web.credit.CreditResource.CreateCreditRequest;
import com.godaddy.vps4.web.mailrelay.VmMailRelayResource;
import com.godaddy.vps4.web.security.GDUser;
import com.godaddy.vps4.web.security.RequiresRole;

public class CreditResourceTest {

    private GDUser user;
    private UUID orionGuid = UUID.randomUUID();
    private VirtualMachineCredit vmCredit;
    private CreditService creditService = mock(CreditService.class);
    private VmMailRelayResource vmMailRelayResource = mock(VmMailRelayResource.class);
    private VirtualMachineService vmService = mock(VirtualMachineService.class);
    private DataCenterService dataCenterService = mock(DataCenterService.class);

    private CreditResource getCreditResource() {
        return new CreditResource(user, creditService, vmMailRelayResource, vmService, dataCenterService);
    }

    private VirtualMachineCredit createVmCredit(AccountStatus accountStatus) {
        return new VirtualMachineCredit.Builder()
                .withAccountGuid(orionGuid.toString())
                .withAccountStatus(accountStatus)
                .withShopperID(user.getShopperId())
                .build();
    }

    @Before
    public void setupTest() {
        user = GDUserMock.createShopper();
        vmCredit = createVmCredit(AccountStatus.ACTIVE);
        when(creditService.getVirtualMachineCredit(orionGuid)).thenReturn(vmCredit);
        when(creditService.getVirtualMachineCredits("validUserShopperId", false)).thenReturn(Collections.singletonList(vmCredit));
    }

    @Test
    public void testShopperGetCredit() {
        Vps4Credit credit = getCreditResource().getCredit(orionGuid);
        Assert.assertEquals(orionGuid, credit.orionGuid);
    }

    @Test
    public void testEmployeeGetCredit() {
        user = GDUserMock.createEmployee();
        Vps4Credit credit = getCreditResource().getCredit(orionGuid);
        Assert.assertEquals(orionGuid, credit.orionGuid);

    }

    @Test
    public void testAdminGetCredit() {
        user = GDUserMock.createAdmin();
        Vps4Credit credit = getCreditResource().getCredit(orionGuid);
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
        List<Vps4Credit> credits = getCreditResource().getCredits(false);
        Assert.assertTrue(credits.stream().anyMatch(c -> c.orionGuid.equals(vmCredit.entitlementData.entitlementId)));
    }

    @Test
    public void testOtherShopperGetCredits() {
        user = GDUserMock.createShopper("otherShopperId");
        List<Vps4Credit> credits = getCreditResource().getCredits(false);
        Assert.assertTrue(credits.isEmpty());
    }

    @Test
    public void testGetSuspendedCredits() {
        VirtualMachineCredit suspendedCredit = createVmCredit(AccountStatus.SUSPENDED);
        when(creditService.getVirtualMachineCredits(GDUserMock.DEFAULT_SHOPPER, true))
                .thenReturn(Collections.singletonList(suspendedCredit));
        List<Vps4Credit> credits = getCreditResource().getCredits(true);
        Assert.assertTrue(credits.stream().anyMatch(c -> c.orionGuid.equals(suspendedCredit.entitlementData.entitlementId)));
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
        List<Vps4Credit> credits = getCreditResource().getCredits(false);
        Assert.assertTrue(credits.stream().anyMatch(c -> c.orionGuid.equals(vmCredit.entitlementData.entitlementId)));
    }

    @Test
    public void testCreateCreditRequiresAdmin() {
        try {
            Method method = CreditResource.class.getMethod("createCredit", CreateCreditRequest.class);
            GDUser.Role[] expectedRoles = new GDUser.Role[] {GDUser.Role.ADMIN};
            Assert.assertArrayEquals(expectedRoles, method.getAnnotation(RequiresRole.class).roles());
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
        req.resellerId = 1;
        req.shopperId = "someShopperId";

        user = GDUserMock.createAdmin(null);
        getCreditResource().createCredit(req);

        verify(creditService).createVirtualMachineCredit(
                any(UUID.class), eq(req.shopperId), eq(req.operatingSystem), eq(req.controlPanel),
                eq(req.tier), eq(req.managedLevel), eq(req.monitoring), eq(req.resellerId));
    }

    @Test
    public void testReleaseCreditRequiresAdmin() {
        try {
            Method method = CreditResource.class.getMethod("releaseCredit", UUID.class);
            GDUser.Role[] expectedRoles = new GDUser.Role[] {GDUser.Role.ADMIN};
            Assert.assertArrayEquals(expectedRoles, method.getAnnotation(RequiresRole.class).roles());
        }
        catch(NoSuchMethodException ex) {
            Assert.fail();
        }
    }

    @Test
    public void testReleaseCredit() {
        user = GDUserMock.createAdmin(null);
        UUID creditGuid = UUID.randomUUID();
        UUID vmId = UUID.randomUUID();
        Map<ProductMetaField, String> prodMeta = new HashMap<>();
        prodMeta.put(ProductMetaField.PRODUCT_ID, vmId.toString());
        when(creditService.getProductMeta(creditGuid)).thenReturn(prodMeta);
        MailRelay relay = new MailRelay();
        relay.relays = 223;
        when(vmMailRelayResource.getCurrentMailRelayUsage(vmId)).thenReturn(relay);

        VirtualMachineCredit freeCredit = getCreditResource().releaseCredit(creditGuid);

        verify(creditService).unclaimVirtualMachineCredit(creditGuid, vmId, relay.relays);
        verify(creditService).getVirtualMachineCredit(creditGuid);
        assertEquals(creditService.getVirtualMachineCredit(creditGuid), freeCredit);
    }

    @Test
    public void testGetCreditHistory() {
        user = GDUserMock.createAdmin(null);
        UUID creditGuid = UUID.randomUUID();
        getCreditResource().getHistory(creditGuid);
        verify(vmService, times(1)).getCreditHistory(creditGuid);
    }

}
