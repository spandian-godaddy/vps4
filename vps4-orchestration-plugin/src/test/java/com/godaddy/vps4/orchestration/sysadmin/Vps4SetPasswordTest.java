package com.godaddy.vps4.orchestration.sysadmin;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.UUID;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.MockitoAnnotations;

import com.godaddy.vps4.orchestration.hfs.plesk.UpdateAdminPassword;
import com.godaddy.vps4.orchestration.hfs.sysadmin.SetPassword;
import com.godaddy.vps4.util.Cryptography;
import com.godaddy.vps4.vm.ActionService;
import com.godaddy.vps4.vm.Image.ControlPanel;

import gdg.hfs.orchestration.CommandContext;
import gdg.hfs.vhfs.sysadmin.ChangePasswordRequestBody;
import gdg.hfs.vhfs.sysadmin.SysAdminAction;
import gdg.hfs.vhfs.sysadmin.SysAdminAction.Status;
import gdg.hfs.vhfs.sysadmin.SysAdminService;


public class Vps4SetPasswordTest {

    ActionService actionService = mock(ActionService.class);
    SysAdminService sysAdminService = mock(SysAdminService.class);
    Cryptography cryptography = mock(Cryptography.class);
    @Captor private ArgumentCaptor<SetPassword.Request> setPasswordRequestCaptor;

    Vps4SetPassword command = spy(new Vps4SetPassword(actionService));

    CommandContext context = mock(CommandContext.class);

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);
    }


    @Test
    public void testSetPassword() throws Exception {
        String password = "somenewpassword";
        String controlPanel = "foobar";

        SetPassword.Request setPasswordRequest = new SetPassword.Request();
        setPasswordRequest.hfsVmId = 42;
        setPasswordRequest.usernames = Arrays.asList("user1", "user2", "user3");
        setPasswordRequest.encryptedPassword = "somenewpassword".getBytes();
        setPasswordRequest.controlPanel = controlPanel;

        Vps4SetPassword.Request request = new Vps4SetPassword.Request();
        request.actionId = 12;
        request.setPasswordRequest = setPasswordRequest;
        request.controlPanel = ControlPanel.MYH;

        SysAdminAction action = new SysAdminAction();
        action.vmId = request.setPasswordRequest.hfsVmId;
        action.sysAdminActionId = 73;
        action.status = Status.COMPLETE;

        when(sysAdminService.changePassword(eq(0), eq(null), eq(null), eq(null), any(ChangePasswordRequestBody.class)))
                .thenReturn(action);
        when(sysAdminService.getSysAdminAction(action.sysAdminActionId)).thenReturn(action);
        when(cryptography.decrypt(any())).thenReturn(password);

        command.executeWithAction(context, request);

        for (String ignored : setPasswordRequest.usernames) {
            verify(context, times(1)).execute(eq(SetPassword.class), setPasswordRequestCaptor.capture());
            SetPassword.Request actualPasswordReq = setPasswordRequestCaptor.getValue();
            Assert.assertEquals(request.setPasswordRequest, actualPasswordReq);
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

        command.executeWithAction(context, request);

        verify(context, times(1)).execute(SetPassword.class, setPasswordReq);
        verify(context, times(1)).execute(UpdateAdminPassword.class, updateAdminReq);
    }

}
