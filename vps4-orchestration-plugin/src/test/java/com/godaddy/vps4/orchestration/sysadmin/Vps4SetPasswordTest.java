package com.godaddy.vps4.orchestration.sysadmin;

import static org.mockito.Mockito.*;

import java.util.Arrays;

import org.junit.Test;

import com.godaddy.vps4.orchestration.TestCommandContext;
import com.godaddy.vps4.orchestration.hfs.sysadmin.SetPassword;
import com.godaddy.vps4.orchestration.hfs.sysadmin.WaitForSysAdminAction;
import com.godaddy.vps4.vm.ActionService;
import com.godaddy.vps4.vm.VmUserService;
import com.google.inject.Guice;
import com.google.inject.Injector;

import gdg.hfs.orchestration.CommandContext;
import gdg.hfs.orchestration.GuiceCommandProvider;
import gdg.hfs.vhfs.sysadmin.SysAdminAction;
import gdg.hfs.vhfs.sysadmin.SysAdminService;
import gdg.hfs.vhfs.sysadmin.SysAdminAction.Status;

public class Vps4SetPasswordTest {

    ActionService actionService = mock(ActionService.class);
    SysAdminService sysAdminService = mock(SysAdminService.class);
    VmUserService userService = mock(VmUserService.class);

    Vps4SetPassword command = new Vps4SetPassword(actionService);

    Injector injector = Guice.createInjector(binder -> {
        binder.bind(SetPassword.class);
        binder.bind(WaitForSysAdminAction.class);
        binder.bind(SysAdminService.class).toInstance(sysAdminService);
    });

    CommandContext context = new TestCommandContext(new GuiceCommandProvider(injector));

    @Test
    public void testSetPassword() throws Exception {

        SetPassword.Request setPasswordRequest = new SetPassword.Request();
        setPasswordRequest.vmId = 42;
        setPasswordRequest.usernames = Arrays.asList("user1", "user2", "user3");
        setPasswordRequest.password = "somenewpassword";

        Vps4SetPassword.Request request = new Vps4SetPassword.Request();
        request.actionId = 12;
        request.setPasswordRequest = setPasswordRequest;

        SysAdminAction action = new SysAdminAction();
        action.vmId = request.setPasswordRequest.vmId;
        action.sysAdminActionId = 73;
        action.status = Status.COMPLETE;

        when(sysAdminService.changePassword(eq(setPasswordRequest.vmId), anyString(), eq(setPasswordRequest.password))).thenReturn(action);
        when(sysAdminService.getSysAdminAction(action.sysAdminActionId)).thenReturn(action);

        command.execute(context, request);

        for (String username : setPasswordRequest.usernames) {
            verify(sysAdminService, times(1))
                .changePassword(42, username, "somenewpassword");
        }
    }

}
