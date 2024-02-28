package com.godaddy.vps4.orchestration.sysadmin;

import com.godaddy.vps4.orchestration.hfs.sysadmin.SetPassword;
import com.godaddy.vps4.util.Cryptography;
import com.godaddy.vps4.vm.ActionService;
import gdg.hfs.orchestration.CommandContext;
import com.godaddy.hfs.sysadmin.ChangePasswordRequestBody;
import com.godaddy.hfs.sysadmin.SysAdminAction;
import com.godaddy.hfs.sysadmin.SysAdminAction.Status;
import com.godaddy.hfs.sysadmin.SysAdminService;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.Arrays;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;


public class Vps4SetSupportUserPasswordTest {

    @Mock private ActionService actionService;
    @Mock private SysAdminService sysAdminService;
    @Mock private Cryptography cryptography;
    @Captor private ArgumentCaptor<SetPassword.Request> setPasswordRequestCaptor;
    @Mock private CommandContext context;
    Vps4SetSupportUserPassword command = spy(new Vps4SetSupportUserPassword(actionService));

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);
    }

    @Test
    public void testSetSupportUserPassword() throws Exception {
        String password = "somenewpassword";

        SetPassword.Request setPasswordRequest = new SetPassword.Request();
        setPasswordRequest.hfsVmId = 42;
        setPasswordRequest.usernames = Arrays.asList("user1", "user2", "user3");
        setPasswordRequest.encryptedPassword = "somenewpassword".getBytes();

        Vps4SetSupportUserPassword.Request request = new Vps4SetSupportUserPassword.Request();
        request.actionId = 12;
        request.setPasswordRequest = setPasswordRequest;

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
}
