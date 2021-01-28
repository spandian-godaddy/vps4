package com.godaddy.vps4.web.vm;

import static org.mockito.Matchers.anyLong;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

import org.junit.Before;
import org.junit.Test;

import com.godaddy.vps4.security.GDUserMock;
import com.godaddy.vps4.vm.VirtualMachine;
import com.godaddy.vps4.vm.VirtualMachineService;
import com.godaddy.vps4.web.Vps4NoShopperException;
import com.godaddy.vps4.web.security.GDUser;

public class VmNydusAckResourceTest {

    private VirtualMachineService virtualMachineService;
    private VmResource vmResource;
    private GDUser user;
    private VmNydusAckResource resource;
    private VirtualMachine testVm;

    @Before
    public void setupTest() {
        user = GDUserMock.createShopper();
        virtualMachineService = mock(VirtualMachineService.class);
        vmResource = mock(VmResource.class);
        resource = new VmNydusAckResource(virtualMachineService, vmResource, user);

        UUID vmId = UUID.randomUUID();
        testVm = new VirtualMachine();
        testVm.vmId = vmId;
        testVm.validOn = Instant.now().minus(Duration.ofMinutes(2));
        testVm.canceled = Instant.MAX;
        testVm.validUntil = Instant.MAX;
        testVm.nydusWarningAck = Instant.MAX;
        when(vmResource.getVm(vmId)).thenReturn(testVm);
    }

    @Test
    public void setNydusWarningAck() {
        resource.acknowledgeNydusWarning(testVm.vmId);
        verify(virtualMachineService, times(1)).ackNydusWarning(testVm.vmId);
    }

    @Test
    public void nydusWarningAckAlreadySet() {
        testVm.nydusWarningAck = Instant.now().minus(Duration.ofMinutes(1));
        resource.acknowledgeNydusWarning(testVm.vmId);
        verify(virtualMachineService, never()).ackNydusWarning(testVm.vmId);
    }

    @Test(expected= Vps4NoShopperException.class)
    public void cannotSetNydusWarningAckIfNotShopper() {
        user = GDUserMock.createEmployee();
        resource = new VmNydusAckResource(virtualMachineService, vmResource, user);
        resource.acknowledgeNydusWarning(testVm.vmId);
    }
}
