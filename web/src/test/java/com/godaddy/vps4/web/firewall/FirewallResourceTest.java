package com.godaddy.vps4.web.firewall;

import com.godaddy.vps4.credit.CreditService;
import com.godaddy.vps4.credit.VirtualMachineCredit;
import com.godaddy.vps4.firewall.FirewallDataService;
import com.godaddy.vps4.firewall.FirewallService;
import com.godaddy.vps4.firewall.model.FirewallDetail;
import com.godaddy.vps4.firewall.model.FirewallSite;
import com.godaddy.vps4.firewall.model.FirewallStatus;
import com.godaddy.vps4.firewall.model.VmFirewallSite;
import com.godaddy.vps4.security.GDUserMock;
import com.godaddy.vps4.vm.*;
import com.godaddy.vps4.web.Vps4Exception;
import com.godaddy.vps4.web.security.GDUser;
import com.godaddy.vps4.web.vm.VmResource;
import org.junit.Before;
import org.junit.Test;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.*;
import static org.mockito.Mockito.times;

public class FirewallResourceTest {
    private UUID vmId;

    private FirewallResource resource;
    private VmResource vmResource = mock(VmResource.class);
    private CreditService creditService = mock(CreditService.class);
    private FirewallService firewallService = mock(FirewallService.class);
    private VirtualMachine vm;
    private VirtualMachineCredit credit;
    private DataCenter dataCenter;
    private FirewallSite firewallSite;
    private FirewallDetail firewallDetail;
    private GDUser userShopper;
    private GDUser userEmployee;
    private GDUser userEmployee2Shopper;

    @Before
    public void setupTest() {
        userShopper = GDUserMock.createShopper();
        userEmployee = GDUserMock.createEmployee();
        userEmployee2Shopper = GDUserMock.createEmployee2Shopper();
        vmId = UUID.randomUUID();
        long hfsVmId = 1111;
        ServerType vmServerType = new ServerType();
        vmServerType.platform = ServerType.Platform.OPTIMIZED_HOSTING;
        ServerSpec vmSpec = new ServerSpec();
        vmSpec.serverType = vmServerType;
        vmSpec.ipAddressLimit = 2;
        dataCenter = new DataCenter(1, "phx3");
        vm = new VirtualMachine(vmId,
                hfsVmId,
                UUID.randomUUID(),
                1,
                vmSpec,
                "Unit Test Vm",
                null,
                null,
                Instant.now(),
                Instant.now().plus(24, ChronoUnit.HOURS),
                Instant.now().plus(24, ChronoUnit.HOURS),
                null,
                null,
                0,
                UUID.randomUUID(),
                dataCenter);

        credit = new VirtualMachineCredit.Builder(mock(DataCenterService.class))
                .withAccountStatus(AccountStatus.ACTIVE)
                .withShopperID(userShopper.getShopperId())
                .build();

        firewallSite = new FirewallSite();
        firewallSite.siteId = "fakeSiteId";
        firewallSite.domain = "fakeDomain.com";
        firewallSite.anyCastIP = "0.0.0.0";
        firewallSite.status = FirewallStatus.SUCCESS;
        firewallSite.planId = "fakePlanId";

        firewallDetail = new FirewallDetail();
        firewallDetail.siteId = "fakeSiteDetailId";

        when(vmResource.getVm(vmId)).thenReturn(vm);
        when(creditService.getVirtualMachineCredit(vm.orionGuid)).thenReturn(credit);

        when(firewallService.getFirewallSites(anyString(), anyString(), any())).thenReturn(Collections.singletonList(firewallSite));
        when(firewallService.getFirewallSiteDetail(anyString(), anyString(), anyString(), any())).thenReturn(firewallDetail);
    }

    @Test
    public void testGetFirewallSitesShopper() {
        resource = new FirewallResource(userShopper, vmResource, creditService, firewallService);
        List<FirewallSite> response = resource.getActiveFirewallSites(vmId);
        verify(vmResource, times(1)).getVm(eq(vm.vmId));
        verify(creditService, times(1)).getVirtualMachineCredit(eq(vm.orionGuid));
        verify(firewallService, times(1))
                .getFirewallSites(credit.getShopperId(), userShopper.getToken().getJwt().getParsedString(), vm.vmId);

        assertEquals(1, response.size());
        assertSame(firewallSite, response.get(0));
    }

    @Test
    public void testGetFirewallSitesE2S() {
        resource = new FirewallResource(userEmployee2Shopper, vmResource, creditService, firewallService);
        List<FirewallSite> response = resource.getActiveFirewallSites(vmId);
        verify(vmResource, times(1)).getVm(eq(vm.vmId));
        verify(creditService, times(1)).getVirtualMachineCredit(eq(vm.orionGuid));
        verify(firewallService, times(1))
                .getFirewallSites(credit.getShopperId(), userShopper.getToken().getJwt().getParsedString(), vm.vmId);

        assertEquals(1, response.size());
        assertSame(firewallSite, response.get(0));
    }

    @Test
    public void testGetFirewallSitesEmployee() {
        resource = new FirewallResource(userEmployee, vmResource, creditService, firewallService);
        List<FirewallSite> response = resource.getActiveFirewallSites(vmId);
        verify(vmResource, times(1)).getVm(eq(vm.vmId));
        verify(creditService, times(1)).getVirtualMachineCredit(eq(vm.orionGuid));
        verify(firewallService, times(1))
                .getFirewallSites(credit.getShopperId(), null, vm.vmId);

        assertEquals(1, response.size());
        assertSame(firewallSite, response.get(0));
    }

    @Test
    public void testGetFirewallSiteDetailShopper() {
        resource = new FirewallResource(userShopper, vmResource, creditService, firewallService);
        FirewallDetail response = resource.getFirewallSiteDetail(vmId, firewallDetail.siteId);
        verify(vmResource, times(1)).getVm(eq(vm.vmId));
        verify(creditService, times(1)).getVirtualMachineCredit(eq(vm.orionGuid));
        verify(firewallService, times(1))
                .getFirewallSiteDetail(credit.getShopperId(), userShopper.getToken().getJwt().getParsedString(), firewallDetail.siteId, vm.vmId);

        assertSame(firewallDetail, response);
    }

    @Test
    public void testGetFirewallSiteDetailE2S() {
        resource = new FirewallResource(userEmployee2Shopper, vmResource, creditService, firewallService);
        FirewallDetail response = resource.getFirewallSiteDetail(vmId, firewallDetail.siteId);
        verify(vmResource, times(1)).getVm(eq(vm.vmId));
        verify(creditService, times(1)).getVirtualMachineCredit(eq(vm.orionGuid));
        verify(firewallService, times(1))
                .getFirewallSiteDetail(credit.getShopperId(), userShopper.getToken().getJwt().getParsedString(), firewallDetail.siteId, vm.vmId);

        assertSame(firewallDetail, response);
    }

    @Test
    public void testGetFirewallSiteDetailEmployee() {
        resource = new FirewallResource(userEmployee, vmResource, creditService, firewallService);
        FirewallDetail response = resource.getFirewallSiteDetail(vmId, firewallDetail.siteId);
        verify(vmResource, times(1)).getVm(eq(vm.vmId));
        verify(creditService, times(1)).getVirtualMachineCredit(eq(vm.orionGuid));
        verify(firewallService, times(1))
                .getFirewallSiteDetail(credit.getShopperId(), null, firewallDetail.siteId, vm.vmId);

        assertSame(firewallDetail, response);
    }
}
