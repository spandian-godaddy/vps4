package com.godaddy.vps4.orchestration.hfs.sysadmin;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.Before;
import org.junit.Test;

import com.godaddy.vps4.orchestration.TestCommandContext;
import com.google.inject.Guice;
import com.google.inject.Injector;

import gdg.hfs.orchestration.CommandContext;
import gdg.hfs.orchestration.GuiceCommandProvider;
import gdg.hfs.vhfs.sysadmin.SysAdminAction;
import gdg.hfs.vhfs.sysadmin.SysAdminAction.Status;
import gdg.hfs.vhfs.sysadmin.SysAdminService;

public class AddUserTest {

    SysAdminService sysAdminService;
    AddUser command;
    CommandContext context;
    Injector injector;

    @Before
    public void setup() {
        sysAdminService = mock(SysAdminService.class);
        command = new AddUser(sysAdminService);
        injector = Guice.createInjector(binder -> {
            binder.bind(AddUser.class);
            binder.bind(WaitForSysAdminAction.class);
            binder.bind(SysAdminService.class).toInstance(sysAdminService);
        });
        context = new TestCommandContext(new GuiceCommandProvider(injector));
    }

    @Test
    public void testAddUser() {
        AddUser.Request request = new AddUser.Request();
        request.hfsVmId = 123L;
        request.username = "testuser";
        request.password = "testpass";
        
        SysAdminAction action = new SysAdminAction();
        action.sysAdminActionId = 321;
        action.status = Status.COMPLETE;
        
        when(sysAdminService.addUser(request.hfsVmId, request.username, request.password)).thenReturn(action);
        when(sysAdminService.getSysAdminAction(action.sysAdminActionId)).thenReturn(action);

        command.execute(context, request);

        verify(sysAdminService, times(1)).addUser(request.hfsVmId, request.username, request.password);
    }

}
