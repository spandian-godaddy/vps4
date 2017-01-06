package com.godaddy.vps4.orchestration.sysadmin;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.UUID;

import org.junit.Test;

import com.godaddy.vps4.orchestration.TestCommandContext;
import com.godaddy.vps4.orchestration.hfs.sysadmin.WaitForSysAdminAction;
import com.godaddy.vps4.vm.ActionService;
import com.godaddy.vps4.vm.VmUserService;
import com.google.inject.Guice;
import com.google.inject.Injector;

import gdg.hfs.orchestration.CommandContext;
import gdg.hfs.orchestration.GuiceCommandProvider;
import gdg.hfs.vhfs.sysadmin.SysAdminAction;
import gdg.hfs.vhfs.sysadmin.SysAdminAction.Status;
import gdg.hfs.vhfs.sysadmin.SysAdminService;

public class Vps4ToggleAdminTest {

    ActionService actionService = mock(ActionService.class);
    SysAdminService sysAdminService = mock(SysAdminService.class);
    VmUserService userService = mock(VmUserService.class);

    Vps4ToggleAdmin command = new Vps4ToggleAdmin(actionService, sysAdminService, userService);

    Injector injector = Guice.createInjector(binder -> {
        binder.bind(WaitForSysAdminAction.class);
        binder.bind(SysAdminService.class).toInstance(sysAdminService);
    });

    CommandContext context = new TestCommandContext(new GuiceCommandProvider(injector));

    @Test
    public void testEnableAdmin() throws Exception {

        Vps4ToggleAdmin.Request request = new Vps4ToggleAdmin.Request();
        request.actionId = 12;
        request.enabled = true;
        request.username = "someuser";
        request.vmId = UUID.randomUUID();
        request.hfsVmId = 42;

        SysAdminAction action = new SysAdminAction();
        action.vmId = request.hfsVmId;
        action.sysAdminActionId = 73;
        action.status = Status.COMPLETE;

        when(sysAdminService.enableAdmin(request.hfsVmId, request.username)).thenReturn(action);
        when(sysAdminService.getSysAdminAction(action.sysAdminActionId)).thenReturn(action);

        command.execute(context, request);

        verify(sysAdminService, times(1))
            .enableAdmin(request.hfsVmId, request.username);
    }

    @Test
    public void testDisableAdmin() throws Exception {

        Vps4ToggleAdmin.Request request = new Vps4ToggleAdmin.Request();
        request.actionId = 12;
        request.enabled = false;
        request.username = "someuser";
        request.vmId = UUID.randomUUID();
        request.hfsVmId = 42;

        SysAdminAction action = new SysAdminAction();
        action.vmId = request.hfsVmId;
        action.sysAdminActionId = 73;
        action.status = Status.COMPLETE;

        when(sysAdminService.disableAdmin(request.hfsVmId, request.username)).thenReturn(action);
        when(sysAdminService.getSysAdminAction(action.sysAdminActionId)).thenReturn(action);

        command.execute(context, request);

        verify(sysAdminService, times(1))
            .disableAdmin(request.hfsVmId, request.username);
    }

    @Test(expected=RuntimeException.class)
    public void failCommandIfHfsActionFails() throws Exception {

        Vps4ToggleAdmin.Request request = new Vps4ToggleAdmin.Request();
        request.actionId = 12;
        request.enabled = true;
        request.username = "someuser";
        request.vmId = UUID.randomUUID();
        request.hfsVmId = 42;

        // if HFS throws an exception on enableAdmin, the command should fail
        when(sysAdminService.enableAdmin(request.hfsVmId, request.username))
            .thenThrow(new RuntimeException("HFS broke"));

        command.execute(context, request);
    }

}
