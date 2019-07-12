package com.godaddy.vps4.orchestration.sysadmin;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.UUID;

import org.junit.Before;
import org.junit.Test;

import com.godaddy.vps4.orchestration.TestCommandContext;
import com.godaddy.vps4.orchestration.hfs.SysAdminActionNotCompletedException;
import com.godaddy.vps4.orchestration.hfs.sysadmin.SetHostname;
import com.godaddy.vps4.orchestration.hfs.sysadmin.SetPassword;
import com.godaddy.vps4.orchestration.hfs.sysadmin.WaitForSysAdminAction;
import com.godaddy.vps4.util.Cryptography;
import com.godaddy.vps4.vm.ActionService;
import com.godaddy.vps4.vm.VirtualMachineService;
import com.google.inject.Guice;
import com.google.inject.Injector;

import static org.apache.commons.lang3.exception.ExceptionUtils.getRootCause;

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
    Cryptography cryptography = mock(Cryptography.class);

    Vps4SetHostname command = new Vps4SetHostname(actionService, virtualMachineService);

    Injector injector = Guice.createInjector(binder -> {
        binder.bind(SetPassword.class);
        binder.bind(WaitForSysAdminAction.class);
        binder.bind(SysAdminService.class).toInstance(sysAdminService);
        binder.bind(Cryptography.class).toInstance(cryptography);
    });

    CommandContext context = new TestCommandContext(new GuiceCommandProvider(injector));

    SysAdminAction action = new SysAdminAction();

    SetHostname.Request setHostnameRequest = new SetHostname.Request(42, "newhostname.testing.tld", null);

    Vps4SetHostname.Request request = new Vps4SetHostname.Request();

    @Before
    public void setupTest() {
        request.actionId = 12;
        request.oldHostname = "oldhostname.testing.tld";
        request.vmId = UUID.randomUUID();
        request.setHostnameRequest = setHostnameRequest;
        action.vmId = request.setHostnameRequest.hfsVmId;
        action.sysAdminActionId = 73;

        when(sysAdminService.changeHostname(eq(setHostnameRequest.hfsVmId), eq(setHostnameRequest.hostname), eq(null)))
                .thenReturn(action);
        when(sysAdminService.getSysAdminAction(action.sysAdminActionId)).thenReturn(action);
    }

    @Test
    public void testSetHostnameSuccess() throws Exception {
        action.status = Status.COMPLETE;
        command.execute(context, request);

        verify(sysAdminService, times(1)).changeHostname(42, "newhostname.testing.tld", null);
        verify(virtualMachineService, times(1)).setHostname(request.vmId, "newhostname.testing.tld");
    }

    @Test
    public void testSetHostnameFail() throws Exception {
        // Verify the old hostname is reset in the database upon failure.
        action.status = Status.FAILED;

        try {
            command.execute(context, request);
            Assert.fail();
        } catch (Exception e) {
            verify(virtualMachineService, times(1)).setHostname(request.vmId, "newhostname.testing.tld");
            verify(virtualMachineService, times(1)).setHostname(request.vmId, "oldhostname.testing.tld");
        }
    }

    @Test
    public void testSetHostnameFailWithTwoCauses() throws Exception {
        action.status = Status.FAILED;
        action.message = "error changing hostname because hostname is the same as cpanel account";
        Exception errorThrown = new RuntimeException(new Exception(new SysAdminActionNotCompletedException(action)));
        when(sysAdminService.changeHostname(eq(setHostnameRequest.hfsVmId), eq(setHostnameRequest.hostname), eq(null)))
                .thenThrow(errorThrown);

        try {
            command.execute(context, request);
            Assert.fail();
        } catch (Exception e) {
            assert (e.getMessage().contains(action.message));
            verify(virtualMachineService, times(1)).setHostname(request.vmId, "newhostname.testing.tld");
            verify(virtualMachineService, times(1)).setHostname(request.vmId, "oldhostname.testing.tld");
        }
    }

    @Test
    public void testSetHostnameFailWithOnlyOneCause() throws Exception {
        action.status = Status.FAILED;
        action.message = "error changing hostname because hostname is the same as cpanel account";
        Exception errorThrown = new RuntimeException(new SysAdminActionNotCompletedException(action));
        when(sysAdminService.changeHostname(eq(setHostnameRequest.hfsVmId), eq(setHostnameRequest.hostname), eq(null)))
                .thenThrow(errorThrown);

        try {
            command.execute(context, request);
            Assert.fail();
        } catch (Exception e) {
            assert (e.getMessage().contains(action.message));
            verify(virtualMachineService, times(1)).setHostname(request.vmId, "newhostname.testing.tld");
            verify(virtualMachineService, times(1)).setHostname(request.vmId, "oldhostname.testing.tld");
        }
    }

}
