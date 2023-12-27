package com.godaddy.vps4.phase2.cdn;

import com.godaddy.vps4.cdn.CdnDataService;
import com.godaddy.vps4.cdn.model.VmCdnSite;
import com.godaddy.vps4.cdn.jdbc.JdbcCdnDataService;
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

public class CdnDataServiceTest {

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
                bind(CdnDataService.class).to(JdbcCdnDataService.class);
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
    public void testInsertCdnSite() {
        injector.getInstance(CdnDataService.class).createCdnSite(vm.vmId,
                vm.primaryIpAddress.addressId, "fakedomain.com", "fakeSiteId");
        VmCdnSite vmCdnSite = injector.getInstance(CdnDataService.class).getCdnSiteFromId(vm.vmId, "fakeSiteId");
        String infinity = "+292278994-08-16T23:00:00Z";

        assertEquals("fakedomain.com", vmCdnSite.domain);
        assertEquals(vm.vmId, vmCdnSite.vmId);
        assertEquals("fakeSiteId", vmCdnSite.siteId);
        assertTrue(Instant.now().plusSeconds(120).isAfter(vmCdnSite.validOn));
        assertEquals(infinity, vmCdnSite.validUntil.toString());
    }


    @Test
    public void testGetCdnSiteWrongIdAndVmId() {
        injector.getInstance(CdnDataService.class).createCdnSite(vm.vmId,
                vm.primaryIpAddress.addressId, "fakedomain.com", "fakeSiteId");
        VmCdnSite vmCdnSiteNotFound = injector.getInstance(CdnDataService.class).getCdnSiteFromId(vm.vmId, "fakeSiteId2");
        VmCdnSite vmCdnSiteWrongVmId = injector.getInstance(CdnDataService.class).getCdnSiteFromId(UUID.randomUUID(), "fakeSiteId");

        assertNull(vmCdnSiteNotFound);
        assertNull(vmCdnSiteWrongVmId);
    }

    @Test
    public void testDestroyCdnSite() {
        injector.getInstance(CdnDataService.class).createCdnSite(vm.vmId,
                vm.primaryIpAddress.addressId, "fakedomain.com", "fakeSiteId");
        injector.getInstance(CdnDataService.class).destroyCdnSite(vm.vmId, "fakeSiteId");
        VmCdnSite vmCdnSite = injector.getInstance(CdnDataService.class).getCdnSiteFromId(vm.vmId, "fakeSiteId");
        assertEquals("fakedomain.com", vmCdnSite.domain);
        assertEquals(vm.vmId, vmCdnSite.vmId);
        assertEquals("fakeSiteId", vmCdnSite.siteId);
        assertTrue(Instant.now().plusSeconds(120).isAfter(vmCdnSite.validUntil));
    }

    @Test
    public void testGetActiveCdnSitesOfVm() {
        String infinity = "+292278994-08-16T23:00:00Z";;

        injector.getInstance(CdnDataService.class)
                .createCdnSite(vm.vmId, vm.primaryIpAddress.addressId,"fakedomain1.com", "fakeSiteId1");
        injector.getInstance(CdnDataService.class)
                .createCdnSite(vm.vmId, vm.primaryIpAddress.addressId,"fakedomain2.com", "fakeSiteId2");
        injector.getInstance(CdnDataService.class)
                .createCdnSite(vm.vmId, vm.primaryIpAddress.addressId,"fakedomain3.com", "fakeSiteId3");
        injector.getInstance(CdnDataService.class)
                .destroyCdnSite(vm.vmId, "fakeSiteId3");

        List<VmCdnSite> vmCdnSites = injector.getInstance(CdnDataService.class).getActiveCdnSitesOfVm(vm.vmId);

        assertEquals(2, vmCdnSites.size());
        assertEquals("fakedomain1.com", vmCdnSites.get(0).domain);
        assertEquals("fakedomain2.com", vmCdnSites.get(1).domain);
        assertEquals("fakeSiteId1", vmCdnSites.get(0).siteId);
        assertEquals("fakeSiteId2", vmCdnSites.get(1).siteId);
        assertEquals(vm.vmId, vmCdnSites.get(0).vmId);
        assertEquals(vm.vmId, vmCdnSites.get(1).vmId);
        assertEquals(vm.primaryIpAddress.ipAddress, vmCdnSites.get(0).ipAddress.ipAddress);
        assertEquals(vm.primaryIpAddress.ipAddress, vmCdnSites.get(1).ipAddress.ipAddress);
        assertTrue(Instant.now().plusSeconds(120).isAfter(vmCdnSites.get(0).validOn));
        assertTrue(Instant.now().plusSeconds(120).isAfter(vmCdnSites.get(1).validOn));
        assertEquals(infinity, vmCdnSites.get(0).validUntil.toString());
        assertEquals(infinity, vmCdnSites.get(1).validUntil.toString());
    }
}
