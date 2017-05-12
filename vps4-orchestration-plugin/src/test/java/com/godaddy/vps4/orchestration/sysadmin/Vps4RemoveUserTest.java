package com.godaddy.vps4.orchestration.sysadmin;

import static org.mockito.Matchers.anyObject;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.util.UUID;

import org.junit.Before;
import org.junit.Test;

import com.godaddy.vps4.orchestration.hfs.sysadmin.AddUser;
import com.godaddy.vps4.orchestration.hfs.sysadmin.RemoveUser;
import com.godaddy.vps4.vm.ActionService;
import com.godaddy.vps4.vm.VmUserService;

import gdg.hfs.orchestration.CommandContext;

public class Vps4RemoveUserTest {

    ActionService actionService;
    Vps4RemoveUser command;
    CommandContext context;
    VmUserService vmUserService;

    @Before
    public void setup() {
        actionService = mock(ActionService.class);
        vmUserService = mock(VmUserService.class);
        command = new Vps4RemoveUser(actionService, vmUserService);
        context = mock(CommandContext.class);
    }

    @Test
    public void testVps4RemoveUser() {
        doReturn(null).when(context).execute(eq(AddUser.class), anyObject());

        Vps4RemoveUser.Request req = new Vps4RemoveUser.Request();
        req.hfsVmId = 123;
        req.username = "testuser";
        req.vmId = UUID.randomUUID();

        doNothing().when(vmUserService).deleteUser(req.username, req.vmId);

        command.execute(context, req);

        verify(context, times(1)).execute(eq(RemoveUser.class), anyObject());
        verify(vmUserService, times(1)).deleteUser(req.username, req.vmId);
    }
}
