package com.godaddy.vps4.phase2;

import static org.mockito.Mockito.mock;

import java.util.List;
import java.util.Random;
import java.util.UUID;

import javax.inject.Inject;
import javax.sql.DataSource;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import com.godaddy.vps4.ipblacklist.IpBlacklistService;
import com.godaddy.vps4.jdbc.DatabaseModule;
import com.godaddy.vps4.mailrelay.MailRelayService;
import com.godaddy.vps4.network.IpAddress;
import com.godaddy.vps4.network.IpAddress.IpAddressType;
import com.godaddy.vps4.panopta.PanoptaApiCustomerService;
import com.godaddy.vps4.panopta.PanoptaApiServerService;
import com.godaddy.vps4.scheduler.api.web.SchedulerWebService;
import com.godaddy.vps4.security.GDUserMock;
import com.godaddy.vps4.security.SecurityModule;
import com.godaddy.vps4.security.Vps4User;
import com.godaddy.vps4.security.Vps4UserService;
import com.godaddy.vps4.security.jdbc.AuthorizationException;
import com.godaddy.vps4.snapshot.SnapshotModule;
import com.godaddy.vps4.vm.AccountStatus;
import com.godaddy.vps4.vm.VirtualMachine;
import com.godaddy.vps4.vm.VmAction;
import com.godaddy.vps4.vm.VmModule;
import com.godaddy.vps4.web.Vps4Exception;
import com.godaddy.vps4.web.network.NetworkResource;
import com.godaddy.vps4.web.security.GDUser;
import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Provides;

public class NetworkResourceUserTest {

    @Inject Vps4UserService userService;
    @Inject DataSource dataSource;

    private GDUser user;

    private Injector injector = Guice.createInjector(
            new DatabaseModule(),
            new SecurityModule(),
            new VmModule(),
            new SnapshotModule(),
            new Phase2ExternalsModule(),
            new CancelActionModule(),
            new AbstractModule() {

                @Override
                public void configure() {
                    SchedulerWebService swServ = Mockito.mock(SchedulerWebService.class);
                    bind(SchedulerWebService.class).toInstance(swServ);
                    bind(PanoptaApiCustomerService.class).toInstance(mock(PanoptaApiCustomerService.class));
                    bind(PanoptaApiServerService.class).toInstance(mock(PanoptaApiServerService.class));
                    bind(MailRelayService.class).toInstance(mock(MailRelayService.class));
                    bind(IpBlacklistService.class).toInstance(mock(IpBlacklistService.class));
                }

                @Provides
                public GDUser provideUser() {
                    return user;
                }
            });


    @Before
    public void setupTest() {
        injector.injectMembers(this);
        user = GDUserMock.createShopper();
    }

    @After
    public void teardownTest() {
        SqlTestData.cleanupSqlTestData(dataSource);
    }

    private VirtualMachine createTestVm() {
        UUID orionGuid = UUID.randomUUID();
        Vps4User vps4User = userService.getOrCreateUserForShopper(GDUserMock.DEFAULT_SHOPPER, "1");
        VirtualMachine vm = SqlTestData.insertTestVm(orionGuid, vps4User.getId(), dataSource, "hfs-centos7");
        return vm;
    }

    private IpAddress createSecondaryIp(UUID vmId) {
        double rando = Math.random() * 350;
        long hfsAddressId = (long) rando;
        Random random = new Random();
        String ipAddress = "192.168.1."+random.nextInt(256);
        IpAddress ip = SqlTestData.insertTestIp(hfsAddressId, vmId, ipAddress, IpAddressType.SECONDARY, dataSource);
        return ip;
    }

 // === addIp Tests ===
    public void testAddIp() {
        VirtualMachine vm = createTestVm();
        NetworkResource resource = injector.getInstance(NetworkResource.class);

        VmAction action = resource.addIpAddress(vm.vmId, 4);
        Assert.assertNotNull(action);
    }

    @Test
    public void testShopperAddIp() {
        testAddIp();
    }

    @Test(expected=AuthorizationException.class)
    public void testUnauthorizedShopperAddIp() {
        user = GDUserMock.createShopper("shopperX");
        testAddIp();
    }

    @Test
    public void testEmployeeAddIp() {
        user = GDUserMock.createEmployee();
        testAddIp();
    }


  // === removeIp Tests ===

    public void testRemoveIp() {
        VirtualMachine vm = createTestVm();
        NetworkResource resource = injector.getInstance(NetworkResource.class);

        IpAddress ip = createSecondaryIp(vm.vmId);

        VmAction action = resource.destroyIpAddress(vm.vmId, ip.addressId);
        Assert.assertNotNull(action);
    }

    @Test
    public void testShopperRemoveIp() {
        testRemoveIp();
    }

    @Test(expected=AuthorizationException.class)
    public void testUnauthorizedShopperRemoveIp() {
        user = GDUserMock.createShopper("shopperX");
        testRemoveIp();
    }

    @Test
    public void testEmployeeRemoveIp() {
        user = GDUserMock.createEmployee();
        testRemoveIp();
    }


    // === list ips Tests ===

    public void testListIps() {
        VirtualMachine vm = createTestVm();
        NetworkResource resource = injector.getInstance(NetworkResource.class);

        createSecondaryIp(vm.vmId);

        List<IpAddress> ips = resource.getIpAddresses(vm.vmId);

        // Only expecting 1 because createTestVm doesn't create the primary IP.
        Assert.assertEquals(1, ips.size());
        Assert.assertEquals(IpAddressType.SECONDARY, ips.get(0).ipAddressType);
    }

    @Test
    public void testShopperListIps() {
        testListIps();
    }

    @Test(expected=AuthorizationException.class)
    public void testUnauthorizedShopperListIps() {
        user = GDUserMock.createShopper("shopperX");
        testListIps();
    }

    @Test
    public void testEmployeeListIps() {
        user = GDUserMock.createEmployee();
        testListIps();
    }


    // === Test get 1 ip ===
    public void testGet1Ip() {
        VirtualMachine vm = createTestVm();
        NetworkResource resource = injector.getInstance(NetworkResource.class);

        IpAddress ip = createSecondaryIp(vm.vmId);

        IpAddress returnedIp = resource.getIpAddress(vm.vmId, ip.addressId);

        // Only expecting 1 because createTestVm doesn't create the primary IP.
        Assert.assertEquals(ip.hfsAddressId, returnedIp.hfsAddressId);
    }

    @Test
    public void testShopperGetIp() {
        testGet1Ip();
    }

    @Test(expected=AuthorizationException.class)
    public void testUnauthorizedShopperGetIp() {
        user = GDUserMock.createShopper("shopperX");
        testGet1Ip();
    }

    @Test
    public void testEmployeeGetIp() {
        user = GDUserMock.createEmployee();
        testGet1Ip();
    }

    @Test
    public void testShopperGetIpFailsIfSuspended() {
        Phase2ExternalsModule.mockVmCredit(AccountStatus.SUSPENDED);
        try {
            testGet1Ip();
            Assert.fail("Exception not thrown");
        } catch (Vps4Exception e) {
            Assert.assertEquals("ACCOUNT_SUSPENDED", e.getId());
        }

    }

}
