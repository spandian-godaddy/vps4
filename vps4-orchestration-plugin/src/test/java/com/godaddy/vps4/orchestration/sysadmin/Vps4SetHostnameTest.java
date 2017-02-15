package com.godaddy.vps4.orchestration.sysadmin;

import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.UUID;

import org.junit.Test;

import com.godaddy.vps4.orchestration.TestCommandContext;
import com.godaddy.vps4.orchestration.hfs.sysadmin.SetHostname;
import com.godaddy.vps4.orchestration.hfs.sysadmin.SetPassword;
import com.godaddy.vps4.orchestration.hfs.sysadmin.WaitForSysAdminAction;
import com.godaddy.vps4.vm.ActionService;
import com.godaddy.vps4.vm.VirtualMachineService;
import com.google.inject.Guice;
import com.google.inject.Injector;

import gdg.hfs.orchestration.CommandContext;
import gdg.hfs.orchestration.GuiceCommandProvider;
import gdg.hfs.vhfs.sysadmin.SysAdminAction;
import gdg.hfs.vhfs.sysadmin.SysAdminAction.Status;
import gdg.hfs.vhfs.sysadmin.SysAdminService;
import junit.framework.Assert;

public class Vps4SetHostnameTest {

    ActionService actionService = mock(ActionService.class);
    SysAdminService sysAdminService = mock(SysAdminService.class);
    VirtualMachineService virtualMachineService = mock(VirtualMachineService.class);

    Vps4SetHostname command = new Vps4SetHostname(actionService, virtualMachineService);

    Injector injector = Guice.createInjector(binder -> {
        binder.bind(SetPassword.class);
        binder.bind(WaitForSysAdminAction.class);
        binder.bind(SysAdminService.class).toInstance(sysAdminService);
    });

    CommandContext context = new TestCommandContext(new GuiceCommandProvider(injector));

    @Test
    public void testSetHostnameSuccess() throws Exception {

        SetHostname.Request setHostnameRequest = new SetHostname.Request();
        setHostnameRequest.hfsVmId = 42;
        setHostnameRequest.hostname = "newhostname.testing.tld";

        Vps4SetHostname.Request request = new Vps4SetHostname.Request();
        request.actionId = 12;
        request.oldHostname = "oldhostname.testing.tld";
        request.vmId = UUID.randomUUID();
        request.setHostnameRequest = setHostnameRequest;

        SysAdminAction action = new SysAdminAction();
        action.vmId = request.setHostnameRequest.hfsVmId;
        action.sysAdminActionId = 73;
        action.status = Status.COMPLETE;

        when(sysAdminService.changeHostname(eq(setHostnameRequest.hfsVmId), eq(setHostnameRequest.hostname), eq(null))).thenReturn(action);
        when(sysAdminService.getSysAdminAction(action.sysAdminActionId)).thenReturn(action);

        command.execute(context, request);

        verify(sysAdminService, times(1)).changeHostname(42, "newhostname.testing.tld", null);
        verify(virtualMachineService, times(1)).setHostname(request.vmId, "newhostname.testing.tld");
    }

    @Test
    public void testSetHostnameFail() throws Exception {
        // Verify the old hostname is reset in the database upon failure.

        SetHostname.Request setHostnameRequest = new SetHostname.Request();
        setHostnameRequest.hfsVmId = 42;
        setHostnameRequest.hostname = "newhostname.testing.tld";

        Vps4SetHostname.Request request = new Vps4SetHostname.Request();
        request.actionId = 12;
        request.oldHostname = "oldhostname.testing.tld";
        request.vmId = UUID.randomUUID();
        request.setHostnameRequest = setHostnameRequest;

        SysAdminAction action = new SysAdminAction();
        action.vmId = request.setHostnameRequest.hfsVmId;
        action.sysAdminActionId = 73;
        action.status = Status.FAILED;

        when(sysAdminService.changeHostname(eq(setHostnameRequest.hfsVmId), eq(setHostnameRequest.hostname), eq(null))).thenReturn(action);
        when(sysAdminService.getSysAdminAction(action.sysAdminActionId)).thenReturn(action);

        try{
            command.execute(context, request);
            Assert.fail();
        }catch(Exception e){
            verify(virtualMachineService, times(1)).setHostname(request.vmId, "newhostname.testing.tld");
            verify(virtualMachineService, times(1)).setHostname(request.vmId, "oldhostname.testing.tld");
        }
    }

}
