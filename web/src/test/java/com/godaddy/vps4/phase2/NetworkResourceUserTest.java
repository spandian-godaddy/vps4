package com.godaddy.vps4.phase2;

import java.util.List;
import java.util.UUID;

import javax.inject.Inject;
import javax.sql.DataSource;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import com.godaddy.vps4.credit.CreditService;
import com.godaddy.vps4.jdbc.DatabaseModule;
import com.godaddy.vps4.network.IpAddress;
import com.godaddy.vps4.network.IpAddress.IpAddressType;
import com.godaddy.vps4.security.GDUserMock;
import com.godaddy.vps4.security.SecurityModule;
import com.godaddy.vps4.security.Vps4User;
import com.godaddy.vps4.security.Vps4UserService;
import com.godaddy.vps4.security.jdbc.AuthorizationException;
import com.godaddy.vps4.vm.Action;
import com.godaddy.vps4.vm.VirtualMachine;
import com.godaddy.vps4.vm.VmModule;
import com.godaddy.vps4.web.network.NetworkResource;
import com.godaddy.vps4.web.security.GDUser;
import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Provides;

import gdg.hfs.orchestration.CommandGroupSpec;
import gdg.hfs.orchestration.CommandService;
import gdg.hfs.orchestration.CommandState;
import gdg.hfs.vhfs.vm.Vm;
import gdg.hfs.vhfs.vm.VmService;

public class NetworkResourceUserTest {

    @Inject Vps4UserService userService;
    @Inject DataSource dataSource;

    private GDUser user;
    private CreditService creditService = Mockito.mock(CreditService.class);
    private Vm hfsVm;
    private long hfsVmId = 98765;

    private Injector injector = Guice.createInjector(
            new DatabaseModule(),
            new SecurityModule(),
            new VmModule(),
            new AbstractModule() {

                @Override
                public void configure() {
                    bind(CreditService.class).toInstance(creditService);

                    // HFS services
                    hfsVm = new Vm();
                    hfsVm.status = "ACTIVE";
                    hfsVm.vmId = hfsVmId;
                    VmService vmService = Mockito.mock(VmService.class);
                    Mockito.when(vmService.getVm(Mockito.anyLong())).thenReturn(hfsVm);
                    bind(VmService.class).toInstance(vmService);

                    // Command Service
                    CommandService commandService = Mockito.mock(CommandService.class);
                    CommandState commandState = new CommandState();
                    commandState.commandId = UUID.randomUUID();
                    Mockito.when(commandService.executeCommand(Mockito.any(CommandGroupSpec.class)))
                            .thenReturn(commandState);
                    bind(CommandService.class).toInstance(commandService);
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

//    private VmResource getNetworkResource() {
//        return injector.getInstance(VmResource.class);
//    }

    private VirtualMachine createTestVm() {
        UUID orionGuid = UUID.randomUUID();
        Vps4User vps4User = userService.getOrCreateUserForShopper(GDUserMock.DEFAULT_SHOPPER);
        VirtualMachine vm = SqlTestData.insertTestVm(orionGuid, vps4User.getId(), dataSource);
        return vm;
    }

    private IpAddress createSecondaryIp(UUID vmId) {
        double rando = Math.random() * 350;
        long ipAddressId = (long) rando;
        String ipAddress = "1.2.3."+(int) rando;
        IpAddress ip = SqlTestData.insertTestIp(ipAddressId, vmId, ipAddress, IpAddressType.SECONDARY, dataSource);
        return ip;
    }

 // === addIp Tests ===
    public void testAddIp() {
        VirtualMachine vm = createTestVm();
        NetworkResource resource = injector.getInstance(NetworkResource.class);

        Action action = resource.addIpAddress(vm.vmId);
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

        Action action = resource.destroyIpAddress(vm.vmId, ip.ipAddressId);
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

        IpAddress ip = createSecondaryIp(vm.vmId);

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

        IpAddress returnedIp = resource.getIpAddress(vm.vmId,ip.ipAddressId);

        // Only expecting 1 because createTestVm doesn't create the primary IP.
        Assert.assertEquals(ip.ipAddressId, returnedIp.ipAddressId);
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

}
