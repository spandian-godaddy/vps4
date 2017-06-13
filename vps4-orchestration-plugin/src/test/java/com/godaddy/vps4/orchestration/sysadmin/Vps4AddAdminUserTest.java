package com.godaddy.vps4.orchestration.sysadmin;

import static org.mockito.Matchers.anyObject;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.UUID;

import org.junit.Before;
import org.junit.Test;

import com.godaddy.vps4.orchestration.hfs.sysadmin.AddUser;
import com.godaddy.vps4.orchestration.hfs.sysadmin.ToggleAdmin;
import com.godaddy.vps4.vm.ActionService;
import com.godaddy.vps4.vm.VmUserService;

import gdg.hfs.orchestration.CommandContext;

public class Vps4AddAdminUserTest {

    ActionService actionService;
    VmUserService vmUserService;
    Vps4AddAdminUser command;
    CommandContext context;

    @Before
    public void setup() {
        actionService = mock(ActionService.class);
        vmUserService = mock(VmUserService.class);
        command = new Vps4AddAdminUser(actionService, vmUserService);
        context = mock(CommandContext.class);
        when(context.getId()).thenReturn(UUID.randomUUID());
    }

    @Test
    public void testVps4AddAdminUser() {
        doReturn(null).when(context).execute(eq(AddUser.class), anyObject());
        doReturn(null).when(context).execute(eq(ToggleAdmin.class), anyObject());

        Vps4AddAdminUser.Request req = new Vps4AddAdminUser.Request();
        req.hfsVmId = 123;
        req.vmId = UUID.randomUUID();
        req.username = "testuser";
        req.password = "testpw";

        doNothing().when(vmUserService).createUser(req.username, req.vmId, true);

        command.execute(context, req);

        verify(context, times(1)).execute(eq(AddUser.class), anyObject());
        verify(context, times(1)).execute(eq(ToggleAdmin.class), anyObject());
        verify(vmUserService, times(1)).createUser(req.username, req.vmId, true);
    }
}
