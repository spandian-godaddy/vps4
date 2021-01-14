package com.godaddy.vps4.orchestration.console;

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.UUID;

import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;

import com.godaddy.hfs.vm.ConsoleRequest;
import com.godaddy.hfs.vm.VmAction;
import com.godaddy.hfs.vm.VmService;
import com.godaddy.vps4.orchestration.TestCommandContext;
import com.godaddy.vps4.orchestration.hfs.vm.WaitForVmAction;
import com.godaddy.vps4.vm.ActionService;
import com.godaddy.vps4.vm.VirtualMachine;
import com.google.inject.Guice;
import com.google.inject.Injector;

import gdg.hfs.orchestration.CommandContext;
import gdg.hfs.orchestration.GuiceCommandProvider;

public class Vps4RequestConsoleTest {
    ActionService actionService = mock(ActionService.class);
    VirtualMachine vm = mock(VirtualMachine.class);
    VmAction hfsAction = mock(VmAction.class);
    VmService vmService = mock(VmService.class);
    WaitForVmAction waitAction = mock(WaitForVmAction.class);

    Vps4RequestConsole command = new Vps4RequestConsole(actionService, vmService);

    Injector injector = Guice.createInjector(binder -> binder.bind(WaitForVmAction.class).toInstance(waitAction));
    CommandContext context = spy(new TestCommandContext(new GuiceCommandProvider(injector)));

    @Before
    public void setup() {
        vm.vmId = UUID.randomUUID();
        vm.hfsVmId = (long) (Math.random() * 1000);
        when(vmService.createConsoleUrl(eq(vm.hfsVmId), any())).thenReturn(hfsAction);
    }

    @Test
    public void testVps4RequestConsole() {
        Vps4RequestConsole.Request request = new Vps4RequestConsole.Request();
        request.vmId = vm.vmId;
        request.hfsVmId = vm.hfsVmId;
        request.fromIpAddress = "8.8.8.8";

        command.executeWithAction(context, request);

        ArgumentCaptor<ConsoleRequest> captor = ArgumentCaptor.forClass(ConsoleRequest.class);
        verify(vmService, times(1)).createConsoleUrl(eq(request.hfsVmId), captor.capture());
        assertEquals(request.fromIpAddress, captor.getValue().allowedAddress);
        verify(context, times(1)).execute(WaitForVmAction.class, hfsAction);
    }
}
