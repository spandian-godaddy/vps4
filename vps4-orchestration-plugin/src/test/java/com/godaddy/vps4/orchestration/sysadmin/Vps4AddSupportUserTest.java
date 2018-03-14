package com.godaddy.vps4.orchestration.sysadmin;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyObject;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.UUID;
import java.util.function.Function;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import com.godaddy.vps4.orchestration.hfs.sysadmin.AddUser;
import com.godaddy.vps4.orchestration.hfs.sysadmin.ToggleAdmin;
import com.godaddy.vps4.orchestration.scheduler.ScheduleSupportUserRemoval;
import com.godaddy.vps4.vm.ActionService;
import com.godaddy.vps4.vm.VmUserService;
import com.godaddy.vps4.vm.VmUserType;

import gdg.hfs.orchestration.CommandContext;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.MockitoAnnotations;

public class Vps4AddSupportUserTest {

    ActionService actionService;
    VmUserService vmUserService;
    Vps4AddSupportUser command;
    CommandContext context;
    @Captor private ArgumentCaptor<Function<CommandContext, Void>> addUserToDatabaseCaptor;

    @Before
    public void setup() {
        MockitoAnnotations.initMocks(this);
        actionService = mock(ActionService.class);
        vmUserService = mock(VmUserService.class);
        command = new Vps4AddSupportUser(actionService, vmUserService);
        context = mock(CommandContext.class);
        when(context.getId()).thenReturn(UUID.randomUUID());
        when(context.execute(eq("AddSupportUserToDatabase"), any(Function.class), eq(Void.class))).thenReturn(null);
    }

    @Test
    public void testVps4AddSupportUser() {
        doReturn(null).when(context).execute(eq(AddUser.class), anyObject());
        doReturn(null).when(context).execute(eq(ToggleAdmin.class), anyObject());
        doReturn(null).when(context).execute(eq(ScheduleSupportUserRemoval.class), anyObject());

        Vps4AddSupportUser.Request req = new Vps4AddSupportUser.Request();
        req.hfsVmId = 123;
        req.vmId = UUID.randomUUID();
        req.username = "testuser";
        req.encryptedPassword = "testpw".getBytes();

        doNothing().when(vmUserService).createUser(req.username, req.vmId, true);

        command.execute(context, req);

        verify(context, times(1)).execute(eq(AddUser.class), anyObject());
        verify(context, times(1)).execute(eq(ToggleAdmin.class), anyObject());
        verify(context, times(1)).execute(eq(ScheduleSupportUserRemoval.class), anyObject());
        verify(context, times(1)).execute(eq("AddSupportUserToDatabase"), addUserToDatabaseCaptor.capture(), eq(Void.class));
        Function<CommandContext, Void> lambda = addUserToDatabaseCaptor.getValue();
        lambda.apply(context);
        verify(vmUserService, times(1)).createUser(req.username, req.vmId, true, VmUserType.SUPPORT);
    }
}
