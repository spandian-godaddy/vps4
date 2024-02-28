package com.godaddy.vps4.web.vm;

import com.godaddy.hfs.vm.Bootscript;
import com.godaddy.hfs.vm.VmService;
import com.godaddy.vps4.security.GDUserMock;
import com.godaddy.vps4.vm.Action;
import com.godaddy.vps4.vm.ActionType;
import com.godaddy.vps4.vm.ActionStatus;
import com.godaddy.vps4.vm.ActionService;
import com.godaddy.vps4.vm.ServerType;
import com.godaddy.vps4.vm.VirtualMachine;
import com.godaddy.vps4.vm.ServerSpec;
import com.godaddy.vps4.web.Vps4NoShopperException;
import com.godaddy.vps4.web.security.GDUser;
import gdg.hfs.orchestration.CommandService;
import gdg.hfs.orchestration.CommandState;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.time.Instant;
import java.util.UUID;

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.anyLong;
import static org.mockito.Matchers.anyObject;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.times;

@RunWith(MockitoJUnitRunner.class)
public class NydusResourceTest {
    @Mock private VmService vmService;
    @Mock private VmResource vmResource;
    @Mock private ActionService actionService;
    @Mock private CommandService commandService;
    private GDUser user;
    private NydusResource resource;
    private VirtualMachine vm;
    private final String version = "1.2.3";

    @Before
    public void setupTest() {
        user = GDUserMock.createAdmin();
        resource = new NydusResource(vmResource, vmService, actionService, commandService, user);

        UUID vmId = UUID.randomUUID();
        vm = new VirtualMachine();
        vm.vmId = vmId;
        vm.hostname = "fake.hostname.com";
        vm.hfsVmId = 40;
        vm.spec = new ServerSpec();
        vm.spec.serverType = new ServerType();
        vm.spec.serverType.platform = ServerType.Platform.OPENSTACK;
        when(vmResource.getVm(vmId)).thenReturn(vm);

        Action testAction = new Action(123L, vmId, ActionType.UPDATE_NYDUS, null, null, null,
                ActionStatus.COMPLETE, Instant.now(), Instant.now(), null, UUID.randomUUID(),
                null);
        when(actionService.getAction(anyLong())).thenReturn(testAction);
        when(actionService.createAction(vmId, ActionType.UPDATE_NYDUS, null, user.getUsername()))
                .thenReturn(testAction.id);
        when(commandService.executeCommand(anyObject())).thenReturn(new CommandState());
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
        resource = new NydusResource(vmResource, vmService, actionService, commandService, user);
        resource.getBootscript(vm.vmId);
        verify(vmService, times(1)).getBootscript(vm.hfsVmId, vm.hostname, true);
    }

    @Test(expected= Vps4NoShopperException.class)
    public void cannotGetBootscriptIfNotShopper() {
        user = GDUserMock.createEmployee();
        resource = new NydusResource(vmResource, vmService, actionService, commandService, user);
        resource.getBootscript(vm.vmId);
    }

    @Test(expected= Vps4NoShopperException.class)
    public void cannotUpgradeNydusIfNotShopper() {
        user = GDUserMock.createEmployee();
        resource = new NydusResource(vmResource, vmService, actionService, commandService, user);
        resource.upgradeNydus(vm.vmId, version);
    }

    @Test
    public void upgradeNydusCallsGetVm() {
        resource.upgradeNydus(vm.vmId, version);
        verify(vmResource, times(1)).getVm(vm.vmId);
    }

    @Test
    public void upgradeNydusCreatesAction() {
        resource.upgradeNydus(vm.vmId, version);
        verify(actionService, times(1))
                .createAction(vm.vmId, ActionType.UPDATE_NYDUS, "{}", user.getUsername());
    }
}
