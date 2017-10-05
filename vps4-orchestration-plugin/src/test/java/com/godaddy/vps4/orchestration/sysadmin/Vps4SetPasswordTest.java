package com.godaddy.vps4.orchestration.sysadmin;

import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.UUID;

import org.junit.Test;

import com.godaddy.vps4.orchestration.TestCommandContext;
import com.godaddy.vps4.orchestration.hfs.plesk.UpdateAdminPassword;
import com.godaddy.vps4.orchestration.hfs.sysadmin.SetPassword;
import com.godaddy.vps4.orchestration.hfs.sysadmin.WaitForSysAdminAction;
import com.godaddy.vps4.vm.ActionService;
import com.godaddy.vps4.vm.Image.ControlPanel;
import com.godaddy.vps4.vm.VmUserService;
import com.google.inject.Guice;
import com.google.inject.Injector;

import gdg.hfs.orchestration.CommandContext;
import gdg.hfs.orchestration.GuiceCommandProvider;
import gdg.hfs.vhfs.sysadmin.SysAdminAction;
import gdg.hfs.vhfs.sysadmin.SysAdminAction.Status;
import gdg.hfs.vhfs.sysadmin.SysAdminService;

public class Vps4SetPasswordTest {

    ActionService actionService = mock(ActionService.class);
    SysAdminService sysAdminService = mock(SysAdminService.class);
    VmUserService userService = mock(VmUserService.class);

    Vps4SetPassword command = spy(new Vps4SetPassword(actionService));

    Injector injector = Guice.createInjector(binder -> {
        binder.bind(SetPassword.class);
        binder.bind(WaitForSysAdminAction.class);
        binder.bind(SysAdminService.class).toInstance(sysAdminService);
    });

    CommandContext context = new TestCommandContext(new GuiceCommandProvider(injector));

    @Test
    public void testSetPassword() throws Exception {

        SetPassword.Request setPasswordRequest = new SetPassword.Request();
        setPasswordRequest.hfsVmId = 42;
        setPasswordRequest.usernames = Arrays.asList("user1", "user2", "user3");
        setPasswordRequest.password = "somenewpassword";

        Vps4SetPassword.Request request = new Vps4SetPassword.Request();
        request.actionId = 12;
        request.setPasswordRequest = setPasswordRequest;
        request.controlPanel = ControlPanel.MYH;

        SysAdminAction action = new SysAdminAction();
        action.vmId = request.setPasswordRequest.hfsVmId;
        action.sysAdminActionId = 73;
        action.status = Status.COMPLETE;

        when(sysAdminService.changePassword(eq(setPasswordRequest.hfsVmId), anyString(), eq(setPasswordRequest.password))).thenReturn(action);
        when(sysAdminService.getSysAdminAction(action.sysAdminActionId)).thenReturn(action);

        command.execute(context, request);

        for (String username : setPasswordRequest.usernames) {
            verify(sysAdminService, times(1))
                .changePassword(42, username, "somenewpassword");
        }
    }

    @Test
    public void testSetPasswordWithPlesk() throws Exception {
        CommandContext context = mock(CommandContext.class);
        when(context.getId()).thenReturn(UUID.randomUUID());
        SetPassword.Request setPasswordReq = mock(SetPassword.Request.class);
        UpdateAdminPassword.Request updateAdminReq = mock(UpdateAdminPassword.Request.class);

        Vps4SetPassword.Request request = new Vps4SetPassword.Request();
        request.actionId = 12;
        request.setPasswordRequest = setPasswordReq;
        request.controlPanel = ControlPanel.PLESK;

        doReturn(updateAdminReq).when(command).makeUpdateAdminRequest(eq(request));

        command.execute(context, request);

        verify(context, times(1)).execute(SetPassword.class, setPasswordReq);
        verify(context, times(1)).execute(UpdateAdminPassword.class, updateAdminReq);
    }

}
