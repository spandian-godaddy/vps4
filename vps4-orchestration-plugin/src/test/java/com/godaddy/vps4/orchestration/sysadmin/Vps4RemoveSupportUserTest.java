package com.godaddy.vps4.orchestration.sysadmin;

import static org.junit.Assert.fail;
import static org.mockito.Matchers.anyLong;
import static org.mockito.Matchers.anyObject;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.UUID;

import org.junit.Before;
import org.junit.Test;

import com.godaddy.hfs.vm.Vm;
import com.godaddy.hfs.vm.VmService;
import com.godaddy.vps4.orchestration.hfs.ActionNotCompletedException;
import com.godaddy.vps4.orchestration.hfs.sysadmin.AddUser;
import com.godaddy.vps4.orchestration.hfs.sysadmin.RemoveUser;
import com.godaddy.vps4.orchestration.scheduler.ScheduleSupportUserRemoval;
import com.godaddy.vps4.vm.ActionService;
import com.godaddy.vps4.vm.VmUserService;

import gdg.hfs.orchestration.CommandContext;

public class Vps4RemoveSupportUserTest {

    ActionService actionService;
    Vps4RemoveSupportUser command;
    CommandContext context;
    VmUserService vmUserService;
    VmService vmService;

    private Vm vm;

    @Before
    public void setup() {
        actionService = mock(ActionService.class);
        vmUserService = mock(VmUserService.class);
        vmService = mock(VmService.class);
        command = new Vps4RemoveSupportUser(actionService, vmUserService, vmService);
        context = mock(CommandContext.class);
        when(context.getId()).thenReturn(UUID.randomUUID());
        vm = createVm(123);
    }

    private Vm createVm(long hfsVmId) {
        Vm vm = new Vm();
        vm.vmId = hfsVmId;
        vm.resource_uuid = UUID.randomUUID().toString();
        vm.status = "ACTIVE";
        return vm;
    }

    @Test
    public void testVps4RemoveSupportUser() {
        doReturn(null).when(context).execute(eq(AddUser.class), anyObject());
        when(vmService.getVm(anyLong())).thenReturn(vm);

        Vps4RemoveSupportUser.Request req = new Vps4RemoveSupportUser.Request();
        req.hfsVmId = 123;
        req.username = "testuser";
        req.vmId = UUID.randomUUID();

        doNothing().when(vmUserService).deleteUser(req.username, req.vmId);

        command.execute(context, req);

        verify(context, times(1)).execute(eq(RemoveUser.class), anyObject());
        verify(vmUserService, times(1)).deleteUser(req.username, req.vmId);
    }

    @Test
    public void testVps4RemoveSupportUserRescheduleOnError() {
        doReturn(null).when(context).execute(eq(AddUser.class), anyObject());
        when(vmService.getVm(anyLong())).thenReturn(vm);
        when(context.execute(eq(RemoveUser.class), anyObject())).thenThrow(new ActionNotCompletedException("Test exception"));

        Vps4RemoveSupportUser.Request req = new Vps4RemoveSupportUser.Request();
        req.hfsVmId = 123;
        req.username = "testuser";
        req.vmId = UUID.randomUUID();

        try {
            command.execute(context, req);
        }catch (RuntimeException e){
            // Ignore the runtime exception
        }

        verify(context, times(1)).execute(eq(ScheduleSupportUserRemoval.class), anyObject());
    }

    @Test(expected = RuntimeException.class)
    public void testVps4RemoveSupportUserRescheduleOnStoppedVm() {
        doReturn(null).when(context).execute(eq(AddUser.class), anyObject());
        vm.status = "STOPPED";
        when(vmService.getVm(anyLong())).thenReturn(vm);

        Vps4RemoveSupportUser.Request req = new Vps4RemoveSupportUser.Request();
        req.hfsVmId = 123;
        req.username = "testuser";
        req.vmId = UUID.randomUUID();

        try {
            command.execute(context, req);
        }catch (RuntimeException e){
            verify(context, times(1)).execute(eq(ScheduleSupportUserRemoval.class), anyObject());
            verify(context, never()).execute(eq(RemoveUser.class), anyObject());
            throw e;
        }

        fail("Expected exception");

    }
}
