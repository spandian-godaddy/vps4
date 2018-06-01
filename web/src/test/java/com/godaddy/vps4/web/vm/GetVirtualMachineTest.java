package com.godaddy.vps4.web.vm;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import com.godaddy.hfs.config.Config;
import com.godaddy.vps4.credit.CreditService;
import com.godaddy.vps4.credit.VirtualMachineCredit;
import com.godaddy.vps4.network.IpAddress;
import com.godaddy.vps4.network.IpAddress.IpAddressType;
import com.godaddy.vps4.scheduler.api.web.SchedulerWebService;
import com.godaddy.vps4.security.GDUserMock;
import com.godaddy.vps4.security.Vps4User;
import com.godaddy.vps4.security.Vps4UserService;
import com.godaddy.vps4.util.Cryptography;
import com.godaddy.vps4.vm.AccountStatus;
import com.godaddy.vps4.vm.DataCenter;
import com.godaddy.vps4.vm.DataCenterService;
import com.godaddy.vps4.vm.VirtualMachine;
import com.godaddy.vps4.vm.VirtualMachineService;
import com.godaddy.vps4.vm.VirtualMachineType;
import com.godaddy.vps4.web.security.GDUser;

import gdg.hfs.vhfs.vm.Vm;
import gdg.hfs.vhfs.vm.VmService;

public class GetVirtualMachineTest {

    UUID vmId = UUID.randomUUID();
    UUID zombieVmId = UUID.randomUUID();
    VmResource vmResource;
    DataCenter dc;
    Vm hfsVm;
    long hfsVmId = 1234;
    UUID orionGuid = UUID.randomUUID();
    VirtualMachine vm;
    GDUser user;

    @Before
    public void setupTest() {
        Vps4UserService userService = getMockedUserService();
        VmService vmService = getMockedVmService();
        VirtualMachineService virtualMachineService = getMockedVvirtualMachineService();
        CreditService creditService = getMockedCreditService();
        Config config = getMockedConfig();
        Cryptography cryptography = Mockito.mock(Cryptography.class);
        SchedulerWebService schedulerWebService = Mockito.mock(SchedulerWebService.class);
        DataCenterService dcService = Mockito.mock(DataCenterService.class);

        vmResource = new VmResource(user, vmService, userService, virtualMachineService, creditService, null, null,
                null, null, null, config, cryptography, schedulerWebService, dcService);
    }

    private Config getMockedConfig() {
        Config config = Mockito.mock(Config.class);
        when(config.get(Mockito.anyString(), Mockito.anyString())).thenReturn("0");
        when(config.get(Mockito.anyString())).thenReturn("0");
        when(config.getData(Mockito.anyString())).thenReturn("cxPTMJetZeRW5ofrsUp0wecvNKsjf1/NHwllp0JllBM=".getBytes());
        return config;
    }

	private CreditService getMockedCreditService() {
        dc = new DataCenter(5, "testDc");
        VirtualMachineCredit credit = new VirtualMachineCredit(vm.orionGuid, 10, 2, 0, "linux", "myh", null,
                user.getShopperId(), AccountStatus.ACTIVE, dc, vmId, false, "1");
        CreditService creditService = Mockito.mock(CreditService.class);
        when(creditService.getVirtualMachineCredit(vm.orionGuid)).thenReturn(credit);
        return creditService;
	}

	private VirtualMachineService getMockedVvirtualMachineService() {
        IpAddress ipAddress = new IpAddress();
        ipAddress.ipAddressType = IpAddressType.PRIMARY;
        ipAddress.ipAddress = "127.0.0.1";
        ipAddress.ipAddressId = 1;
		VirtualMachineService virtualMachineService = Mockito.mock(VirtualMachineService.class);
        vm = new VirtualMachine(vmId, hfsVmId, orionGuid, 1, null, "Unit Test Vm", null, ipAddress,
                Instant.now(), Instant.now().plus(24, ChronoUnit.HOURS), Instant.now().plus(24, ChronoUnit.HOURS), null,
                0, UUID.randomUUID());

        when(virtualMachineService.getVirtualMachine(vmId)).thenReturn(vm);
        when(virtualMachineService.getVirtualMachines(Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any())).thenReturn(Arrays.asList(vm));
        return virtualMachineService;
	}

	private VmService getMockedVmService() {
        hfsVm = new Vm();
        hfsVm.vmId = hfsVmId;
        VmService vmService = Mockito.mock(VmService.class);
        when(vmService.getVm(hfsVmId)).thenReturn(hfsVm);
        return vmService;
	}

	private Vps4UserService getMockedUserService() {
		user = GDUserMock.createEmployee2Shopper("testshopper");
        Vps4UserService userService = Mockito.mock(Vps4UserService.class);
        Vps4User vps4User = Mockito.mock(Vps4User.class);
        when(vps4User.getId()).thenReturn(1L);
        when(userService.getOrCreateUserForShopper(user.getShopperId(), "1")).thenReturn(vps4User);
        when(userService.getUser(user.getShopperId())).thenReturn(vps4User);
        return userService;
	}

    @Test
    public void testGetVirtualMachineWithDetailsIncludesDataCenter(){
        VirtualMachineWithDetails vm = vmResource.getVirtualMachineWithDetails(vmId);
        assertEquals(dc, vm.dataCenter);
    }

    @Test
    public void testGetVirtualMachineDetails(){
        VirtualMachineDetails vmDetails = vmResource.getVirtualMachineDetails(vmId);
        assertEquals(new Long(hfsVm.vmId), vmDetails.vmId);
    }

    @Test
    public void testGetDetailsNullVm() {
        VirtualMachineDetails details = new VirtualMachineDetails(null);
        assertEquals(null, details.vmId);
        assertEquals("REQUESTING", details.status);
        assertEquals(false, details.running);
        assertEquals(false, details.useable);

        // Verify class has toString method
        details.toString();
    }

    @Test
    public void testGetVirtualMachinesForShopperId(){
        List<VirtualMachine> vms = vmResource.getVirtualMachines(VirtualMachineType.ACTIVE, user.getShopperId(), null, null, null);
        assertEquals(1, vms.size());
        assertEquals(vmId, vms.get(0).vmId);
    }

    @Test
    public void testGetZombieVirtualMachinesForShopperId(){
        List<VirtualMachine> vms = vmResource.getVirtualMachines(VirtualMachineType.ZOMBIE, user.getShopperId(), null, null, null);
        assertEquals(1, vms.size());
        assertEquals(vmId, vms.get(0).vmId);
    }

    @Test
    public void testGetVmByIpAddress() {
        List<VirtualMachine> vms = vmResource.getVirtualMachines(null, null, "127.0.0.1", null, null);
        assertEquals(1, vms.size());
        assertEquals(vmId, vms.get(0).vmId);
    }

    @Test
    public void testGetVmByOrionGuid() {
        List<VirtualMachine> vms = vmResource.getVirtualMachines(null, null, null, orionGuid, null);
        assertEquals(1, vms.size());
        assertEquals(vmId, vms.get(0).vmId);
    }

    @Test
    public void testGetVmByHfsVmId() {
        List<VirtualMachine> vms = vmResource.getVirtualMachines(null, null, null, null, hfsVmId);
        assertEquals(1, vms.size());
        assertEquals(vmId, vms.get(0).vmId);
    }

    @Test
    public void testGetVmByShopperId() {
        List<VirtualMachine> vms = vmResource.getVirtualMachines(null, user.getShopperId(), null, null, null);
        assertEquals(1, vms.size());
        assertEquals(vmId, vms.get(0).vmId);
    }
}
