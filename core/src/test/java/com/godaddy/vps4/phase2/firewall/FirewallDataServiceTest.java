package com.godaddy.vps4.phase2.firewall;

import com.godaddy.vps4.firewall.FirewallDataService;
import com.godaddy.vps4.firewall.model.VmFirewallSite;
import com.godaddy.vps4.firewall.jdbc.JdbcFirewallDataService;
import com.godaddy.vps4.jdbc.DatabaseModule;
import com.godaddy.vps4.phase2.SqlTestData;
import com.godaddy.vps4.security.Vps4User;
import com.godaddy.vps4.vm.VirtualMachine;
import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Injector;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import javax.sql.DataSource;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.junit.Assert.*;

public class FirewallDataServiceTest {

    VirtualMachine vm;
    Vps4User user;

    static private Injector injectorForDS;
    private Injector injector;
    static private DataSource dataSource;

    @BeforeClass
    public static void setUpInternalInjector() {
        injectorForDS = Guice.createInjector(new DatabaseModule());
        dataSource = injectorForDS.getInstance(DataSource.class);
    }

    @Before
    public void setUp() {
        injector = Guice.createInjector(new AbstractModule() {
            @Override
            protected void configure() {
                bind(DataSource.class).toInstance(dataSource);
                bind(FirewallDataService.class).to(JdbcFirewallDataService.class);
            }
        });
        user = SqlTestData.insertTestVps4User(dataSource);
        vm = SqlTestData.insertTestVmWithIp(UUID.randomUUID(), dataSource, user.getId());
    }

    @After
    public void cleanup() {
        SqlTestData.cleanupTestVmAndRelatedData(vm.vmId, dataSource);
        SqlTestData.deleteTestVps4User(dataSource);
    }

    @Test
    public void testInsertFirewallSite() {
        injector.getInstance(FirewallDataService.class).createFirewallSite(vm.vmId,
                vm.primaryIpAddress.addressId, "fakedomain.com", "fakeSiteId");
        VmFirewallSite vmFirewallSite = injector.getInstance(FirewallDataService.class).getFirewallSiteFromId(vm.vmId, "fakeSiteId");
        String infinity = "+292278994-08-16T23:00:00Z";

        assertEquals("fakedomain.com", vmFirewallSite.domain);
        assertEquals(vm.vmId, vmFirewallSite.vmId);
        assertEquals("fakeSiteId", vmFirewallSite.siteId);
        assertTrue(Instant.now().plusSeconds(120).isAfter(vmFirewallSite.validOn));
        assertEquals(infinity, vmFirewallSite.validUntil.toString());
    }


    @Test
    public void testGetFirewallSiteWrongIdAndVmId() {
        injector.getInstance(FirewallDataService.class).createFirewallSite(vm.vmId,
                vm.primaryIpAddress.addressId, "fakedomain.com", "fakeSiteId");
        VmFirewallSite vmFirewallSiteNotFound = injector.getInstance(FirewallDataService.class).getFirewallSiteFromId(vm.vmId, "fakeSiteId2");
        VmFirewallSite vmFirewallSiteWrongVmId = injector.getInstance(FirewallDataService.class).getFirewallSiteFromId(UUID.randomUUID(), "fakeSiteId");

        assertNull(vmFirewallSiteNotFound);
        assertNull(vmFirewallSiteWrongVmId);
    }

    @Test
    public void testDestroyFirewallSite() {
        injector.getInstance(FirewallDataService.class).createFirewallSite(vm.vmId,
                vm.primaryIpAddress.addressId, "fakedomain.com", "fakeSiteId");
        injector.getInstance(FirewallDataService.class).destroyFirewallSite("fakeSiteId");
        VmFirewallSite vmFirewallSite = injector.getInstance(FirewallDataService.class).getFirewallSiteFromId(vm.vmId, "fakeSiteId");
        assertEquals("fakedomain.com", vmFirewallSite.domain);
        assertEquals(vm.vmId, vmFirewallSite.vmId);
        assertEquals("fakeSiteId", vmFirewallSite.siteId);
        assertTrue(Instant.now().plusSeconds(120).isAfter(vmFirewallSite.validUntil));
    }

    @Test
    public void testGetActiveFirewallSitesOfVm() {
        String infinity = "+292278994-08-16T23:00:00Z";;

        injector.getInstance(FirewallDataService.class)
                .createFirewallSite(vm.vmId, vm.primaryIpAddress.addressId,"fakedomain1.com", "fakeSiteId1");
        injector.getInstance(FirewallDataService.class)
                .createFirewallSite(vm.vmId, vm.primaryIpAddress.addressId,"fakedomain2.com", "fakeSiteId2");
        injector.getInstance(FirewallDataService.class)
                .createFirewallSite(vm.vmId, vm.primaryIpAddress.addressId,"fakedomain3.com", "fakeSiteId3");
        injector.getInstance(FirewallDataService.class)
                .destroyFirewallSite("fakeSiteId3");

        List<VmFirewallSite> vmFirewallSites = injector.getInstance(FirewallDataService.class).getActiveFirewallSitesOfVm(vm.vmId);

        assertEquals(2, vmFirewallSites.size());
        assertEquals("fakedomain1.com", vmFirewallSites.get(0).domain);
        assertEquals("fakedomain2.com", vmFirewallSites.get(1).domain);
        assertEquals("fakeSiteId1", vmFirewallSites.get(0).siteId);
        assertEquals("fakeSiteId2", vmFirewallSites.get(1).siteId);
        assertEquals(vm.vmId, vmFirewallSites.get(0).vmId);
        assertEquals(vm.vmId, vmFirewallSites.get(1).vmId);
        assertEquals(vm.primaryIpAddress.ipAddress, vmFirewallSites.get(0).ipAddress.ipAddress);
        assertEquals(vm.primaryIpAddress.ipAddress, vmFirewallSites.get(1).ipAddress.ipAddress);
        assertTrue(Instant.now().plusSeconds(120).isAfter(vmFirewallSites.get(0).validOn));
        assertTrue(Instant.now().plusSeconds(120).isAfter(vmFirewallSites.get(1).validOn));
        assertEquals(infinity, vmFirewallSites.get(0).validUntil.toString());
        assertEquals(infinity, vmFirewallSites.get(1).validUntil.toString());
    }
}
