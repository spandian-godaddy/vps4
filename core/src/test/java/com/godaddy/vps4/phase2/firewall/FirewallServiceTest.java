package com.godaddy.vps4.phase2.firewall;

import com.godaddy.vps4.firewall.FirewallService;
import com.godaddy.vps4.firewall.FirewallSite;
import com.godaddy.vps4.firewall.jdbc.JdbcFirewallService;
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

public class FirewallServiceTest {

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
                bind(FirewallService.class).to(JdbcFirewallService.class);
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
        injector.getInstance(FirewallService.class).createFirewallSite(vm.vmId,
                vm.primaryIpAddress.addressId, "fakedomain.com", "fakeSiteId");
        FirewallSite firewallSite = injector.getInstance(FirewallService.class).getFirewallSiteFromId("fakeSiteId");
        String infinity = "+292278994-08-16T23:00:00Z";

        assertEquals("fakedomain.com", firewallSite.domain);
        assertEquals(vm.vmId, firewallSite.vmId);
        assertEquals("fakeSiteId", firewallSite.siteId);
        assertTrue(Instant.now().plusSeconds(120).isAfter(firewallSite.validOn));
        assertEquals(infinity, firewallSite.validUntil.toString());
    }

    @Test
    public void testDestroyFirewallSite() {
        injector.getInstance(FirewallService.class).createFirewallSite(vm.vmId,
                vm.primaryIpAddress.addressId, "fakedomain.com", "fakeSiteId");
        injector.getInstance(FirewallService.class).destroyFirewallSite("fakeSiteId");
        FirewallSite firewallSite = injector.getInstance(FirewallService.class).getFirewallSiteFromId("fakeSiteId");
        assertEquals("fakedomain.com", firewallSite.domain);
        assertEquals(vm.vmId, firewallSite.vmId);
        assertEquals("fakeSiteId", firewallSite.siteId);
        assertTrue(Instant.now().plusSeconds(120).isAfter(firewallSite.validUntil));
    }

    @Test
    public void testGetActiveFirewallSitesOfVm() {
        String infinity = "+292278994-08-16T23:00:00Z";;

        injector.getInstance(FirewallService.class)
                .createFirewallSite(vm.vmId, vm.primaryIpAddress.addressId,"fakedomain1.com", "fakeSiteId1");
        injector.getInstance(FirewallService.class)
                .createFirewallSite(vm.vmId, vm.primaryIpAddress.addressId,"fakedomain2.com", "fakeSiteId2");
        injector.getInstance(FirewallService.class)
                .createFirewallSite(vm.vmId, vm.primaryIpAddress.addressId,"fakedomain3.com", "fakeSiteId3");
        injector.getInstance(FirewallService.class)
                .destroyFirewallSite("fakeSiteId3");

        List<FirewallSite> firewallSites = injector.getInstance(FirewallService.class).getActiveFirewallSitesOfVm(vm.vmId);

        assertEquals(2, firewallSites.size());
        assertEquals("fakedomain1.com", firewallSites.get(0).domain);
        assertEquals("fakedomain2.com", firewallSites.get(1).domain);
        assertEquals("fakeSiteId1", firewallSites.get(0).siteId);
        assertEquals("fakeSiteId2", firewallSites.get(1).siteId);
        assertEquals(vm.vmId, firewallSites.get(0).vmId);
        assertEquals(vm.vmId, firewallSites.get(1).vmId);
        assertEquals(vm.primaryIpAddress.ipAddress, firewallSites.get(0).ipAddress.ipAddress);
        assertEquals(vm.primaryIpAddress.ipAddress, firewallSites.get(1).ipAddress.ipAddress);
        assertTrue(Instant.now().plusSeconds(120).isAfter(firewallSites.get(0).validOn));
        assertTrue(Instant.now().plusSeconds(120).isAfter(firewallSites.get(1).validOn));
        assertEquals(infinity, firewallSites.get(0).validUntil.toString());
        assertEquals(infinity, firewallSites.get(1).validUntil.toString());
    }
}
