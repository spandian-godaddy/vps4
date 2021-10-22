package com.godaddy.vps4.orchestration.hfs.sysadmin;

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyObject;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import com.godaddy.vps4.orchestration.TestCommandContext;
import com.godaddy.vps4.util.Cryptography;
import com.google.inject.Guice;
import com.google.inject.Injector;

import gdg.hfs.orchestration.CommandContext;
import gdg.hfs.orchestration.GuiceCommandProvider;
import gdg.hfs.vhfs.sysadmin.AddUserRequestBody;
import gdg.hfs.vhfs.sysadmin.SysAdminAction;
import gdg.hfs.vhfs.sysadmin.SysAdminAction.Status;
import gdg.hfs.vhfs.sysadmin.SysAdminService;

@RunWith(MockitoJUnitRunner.class)
public class AddUserTest {

    @Captor private ArgumentCaptor<AddUserRequestBody> addUserCaptor;

    @Mock Cryptography cryptography;
    @Mock SysAdminService sysAdminService;

    AddUser command;
    CommandContext context;
    Injector injector;
    String password = "testpass";

    @Before
    public void setup() {
        when(cryptography.decrypt(anyObject())).thenReturn(password);
        command = new AddUser(sysAdminService, cryptography);
        injector = Guice.createInjector(binder -> {
            binder.bind(AddUser.class).toInstance(command);
            binder.bind(WaitForSysAdminAction.class);
            binder.bind(SysAdminService.class).toInstance(sysAdminService);
            binder.bind(Cryptography.class).toInstance(cryptography);
        });
        context = new TestCommandContext(new GuiceCommandProvider(injector));
    }

    @Test
    public void testAddUser() {
        AddUser.Request request = new AddUser.Request();
        request.hfsVmId = 123L;
        request.username = "testuser";
        request.encryptedPassword = password.getBytes();
        
        SysAdminAction action = new SysAdminAction();
        action.sysAdminActionId = 321;
        action.status = Status.COMPLETE;

        when(sysAdminService.addUser(eq(0L), eq(null), eq(null), any(AddUserRequestBody.class))).thenReturn(action);
        when(sysAdminService.getSysAdminAction(action.sysAdminActionId)).thenReturn(action);

        command.execute(context, request);

        verify(sysAdminService, times(1)).addUser(eq(0L), eq(null), eq(null), addUserCaptor.capture());

        assertEquals(request.hfsVmId, addUserCaptor.getValue().serverId);
        assertEquals(request.username, addUserCaptor.getValue().username);
        assertEquals(password, addUserCaptor.getValue().password);
    }

}
