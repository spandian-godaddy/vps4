package com.godaddy.vps4.orchestration.vm;

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.Test;

import com.godaddy.vps4.orchestration.TestCommandContext;
import com.godaddy.vps4.vm.ActionService;
import com.godaddy.vps4.vm.VirtualMachine;
import com.google.inject.Guice;
import com.google.inject.Injector;

import gdg.hfs.orchestration.CommandContext;
import gdg.hfs.orchestration.GuiceCommandProvider;
import com.godaddy.hfs.vm.VmAction;
import com.godaddy.hfs.vm.VmService;

public class Vps4RestartVmTest {

    ActionService actionService = mock(ActionService.class);
    VmService vmService = mock(VmService.class);
    WaitForManageVmAction waitAction = mock(WaitForManageVmAction.class);

    Vps4RestartVm command = new Vps4RestartVm(actionService, vmService);

    Injector injector = Guice.createInjector(binder -> {
        binder.bind(WaitForManageVmAction.class).toInstance(waitAction);
    });
    CommandContext context = spy(new TestCommandContext(new GuiceCommandProvider(injector)));

    @Test
    public void testExecuteWithActionSuccess() {
        VmActionRequest request = new VmActionRequest();
        VirtualMachine vm = mock(VirtualMachine.class);
        vm.hfsVmId = 42;
        request.virtualMachine = vm;

        VmAction stoppingHfsAction = mock(VmAction.class);
        VmAction startingHfsAction = mock(VmAction.class);
        when(vmService.stopVm(vm.hfsVmId)).thenReturn(stoppingHfsAction);
        when(vmService.startVm(vm.hfsVmId)).thenReturn(startingHfsAction);

        VmAction waitingHfsAction = mock(VmAction.class);
        doReturn(waitingHfsAction).when(context).execute(anyString(), eq(WaitForManageVmAction.class), any(VmAction.class));

        Vps4RestartVm.Response response = command.executeWithAction(context, request);
        verify(vmService, times(1)).stopVm(request.virtualMachine.hfsVmId);
        verify(context, times(1)).execute("WaitForStop", WaitForManageVmAction.class, stoppingHfsAction);
        verify(vmService, times(1)).startVm(request.virtualMachine.hfsVmId);
        verify(context, times(1)).execute("WaitForStart", WaitForManageVmAction.class, startingHfsAction);
        assertEquals(vm.hfsVmId, response.vmId);
        assertEquals(waitingHfsAction, response.hfsAction);
    }

}
