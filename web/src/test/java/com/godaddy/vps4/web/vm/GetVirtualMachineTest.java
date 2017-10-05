package com.godaddy.vps4.web.vm;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import com.godaddy.hfs.config.Config;
import com.godaddy.vps4.credit.CreditService;
import com.godaddy.vps4.credit.VirtualMachineCredit;
import com.godaddy.vps4.security.GDUserMock;
import com.godaddy.vps4.security.Vps4UserService;
import com.godaddy.vps4.vm.AccountStatus;
import com.godaddy.vps4.vm.DataCenter;
import com.godaddy.vps4.vm.VirtualMachine;
import com.godaddy.vps4.vm.VirtualMachineService;
import com.godaddy.vps4.web.security.GDUser;

import gdg.hfs.vhfs.vm.Vm;
import gdg.hfs.vhfs.vm.VmService;

public class GetVirtualMachineTest {

    UUID vmId = UUID.randomUUID();
    VmResource vmResource;
    DataCenter dc;
    Vm hfsVm;

    @Before
    public void setupTest() {
        GDUser user = GDUserMock.createShopper("testshopper");
        Vps4UserService userService = Mockito.mock(Vps4UserService.class);
        long hfsVmId = 1234;
        hfsVm = new Vm();
        hfsVm.vmId = hfsVmId;
        VmService vmService = Mockito.mock(VmService.class);
        when(vmService.getVm(hfsVmId)).thenReturn(hfsVm);
        VirtualMachineService virtualMachineService = Mockito.mock(VirtualMachineService.class);

        VirtualMachine vm = new VirtualMachine(vmId, hfsVmId, UUID.randomUUID(),
                1, null, "Unit Test Vm", null, null,
                Instant.now(), Instant.now().plus(24, ChronoUnit.HOURS),
                null, AccountStatus.ACTIVE);
        when(virtualMachineService.getVirtualMachine(vmId)).thenReturn(vm);

        dc = new DataCenter(5, "testDc");
        VirtualMachineCredit credit = new VirtualMachineCredit(vm.orionGuid, 10, 0, 0, "linux", "myh", Instant.now(),
                null, user.getShopperId(), AccountStatus.ACTIVE, dc, vmId);

        CreditService creditService = Mockito.mock(CreditService.class);
        when(creditService.getVirtualMachineCredit(vm.orionGuid)).thenReturn(credit);

        Config config = Mockito.mock(Config.class);
        when(config.get(Mockito.anyString(), Mockito.anyString())).thenReturn("0");
        when(config.get(Mockito.anyString())).thenReturn("0");
        vmResource = new VmResource(
            user, vmService, userService, virtualMachineService,
                creditService, null, null, null, null,
                null, config);
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

}
