package com.godaddy.vps4.web.vm;

import com.godaddy.hfs.vm.Bootscript;
import com.godaddy.hfs.vm.VmService;
import com.godaddy.vps4.security.GDUserMock;
import com.godaddy.vps4.vm.ServerType;
import com.godaddy.vps4.vm.VirtualMachine;
import com.godaddy.vps4.vm.ServerSpec;
import com.godaddy.vps4.web.Vps4NoShopperException;
import com.godaddy.vps4.web.security.GDUser;
import org.junit.Before;
import org.junit.Test;

import java.util.UUID;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.times;


public class VmBootscriptResourceTest {

    private VmService vmService;
    private VmResource vmResource;
    private GDUser user;
    private VmBootscriptResource resource;
    private VirtualMachine vm;

    @Before
    public void setupTest() {
        user = GDUserMock.createAdmin();
        vmService = mock(VmService.class);
        vmResource = mock(VmResource.class);
        resource = new VmBootscriptResource(vmResource, vmService, user);

        UUID vmId = UUID.randomUUID();
        vm = new VirtualMachine();
        vm.vmId = vmId;
        vm.hostname = "fake.hostname.com";
        vm.hfsVmId = 40;
        vm.spec = new ServerSpec();
        vm.spec.serverType = new ServerType();
        vm.spec.serverType.platform = ServerType.Platform.OPENSTACK;
        when(vmResource.getVm(vmId)).thenReturn(vm);
    }

    @Test
    public void getBootscriptCallsGetVm() {
        resource.getBootscript(vm.vmId);
        verify(vmResource, times(1)).getVm(vm.vmId);
    }

    @Test
    public void getBootscriptCallsHfs() {
        resource.getBootscript(vm.vmId);
        verify(vmService, times(1)).getBootscript(vm.hfsVmId, vm.hostname, true);
    }

    @Test
    public void getBootscriptReturnsHfsBootscript() {
        Bootscript hfsBootscript = new Bootscript();
        hfsBootscript.bootscript = "testBootscript";
        when(vmService.getBootscript(vm.hfsVmId, vm.hostname, true)).thenReturn(hfsBootscript);

        Bootscript bootscript = resource.getBootscript(vm.vmId);
        assertEquals(hfsBootscript.bootscript, bootscript.bootscript);
    }

    @Test
    public void getBootscriptE2S() {
        user = GDUserMock.createEmployee2Shopper();
        resource = new VmBootscriptResource(vmResource, vmService, user);
        resource.getBootscript(vm.vmId);
        verify(vmService, times(1)).getBootscript(vm.hfsVmId, vm.hostname, true);
    }

    @Test(expected= Vps4NoShopperException.class)
    public void cannotGetBootscriptIfNotShopper() {
        user = GDUserMock.createEmployee();
        resource = new VmBootscriptResource(vmResource, vmService, user);
        resource.getBootscript(vm.vmId);
    }
}
