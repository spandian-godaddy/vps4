package com.godaddy.vps4.orchestration.vm;

import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.Test;

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

public class Vps4CreateBackupStorageTest {
    ActionService actionService = mock(ActionService.class);
    VmService vmService = mock(VmService.class);
    WaitForVmAction waitAction = mock(WaitForVmAction.class);

    Vps4CreateBackupStorage command = new Vps4CreateBackupStorage(actionService, vmService);

    Injector injector = Guice.createInjector(binder -> binder.bind(WaitForVmAction.class).toInstance(waitAction));
    CommandContext context = spy(new TestCommandContext(new GuiceCommandProvider(injector)));

    @Test
    public void testVps4CreateBackupStorage() {
        VmActionRequest request = new VmActionRequest();
        VirtualMachine vm = mock(VirtualMachine.class);
        vm.hfsVmId = 42;
        request.virtualMachine = vm;

        VmAction hfsAction = mock(VmAction.class);
        when(vmService.createBackupStorage(vm.hfsVmId)).thenReturn(hfsAction);
        doReturn(hfsAction).when(context).execute(WaitForVmAction.class, hfsAction);

        command.executeWithAction(context, request);
        verify(vmService, times(1)).createBackupStorage(request.virtualMachine.hfsVmId);
        verify(context, times(1)).execute(WaitForVmAction.class, hfsAction);
    }
}
