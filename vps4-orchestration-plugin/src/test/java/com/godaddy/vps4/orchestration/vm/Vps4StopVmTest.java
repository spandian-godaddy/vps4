package com.godaddy.vps4.orchestration.vm;

import static org.junit.Assert.assertEquals;
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
import gdg.hfs.vhfs.vm.VmAction;
import gdg.hfs.vhfs.vm.VmService;

public class Vps4StopVmTest {

    ActionService actionService = mock(ActionService.class);
    VmService vmService = mock(VmService.class);
    WaitForManageVmAction waitAction = mock(WaitForManageVmAction.class);

    Vps4StopVm command = new Vps4StopVm(actionService, vmService);

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

        VmAction hfsAction = mock(VmAction.class);
        when(vmService.stopVm(vm.hfsVmId)).thenReturn(hfsAction);
        doReturn(hfsAction).when(context).execute(WaitForManageVmAction.class, hfsAction);

        Vps4StopVm.Response response = command.executeWithAction(context, request);
        verify(vmService, times(1)).stopVm(request.virtualMachine.hfsVmId);
        verify(context, times(1)).execute(WaitForManageVmAction.class, hfsAction);
        assertEquals(vm.hfsVmId, response.vmId);
        assertEquals(hfsAction, response.hfsAction);
    }
}
