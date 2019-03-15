package com.godaddy.vps4.orchestration.hfs.vm;

import static org.junit.Assert.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.Test;

import com.godaddy.hfs.vm.VmAction;
import com.godaddy.hfs.vm.VmService;
import com.godaddy.vps4.orchestration.TestCommandContext;
import com.godaddy.vps4.orchestration.vm.WaitForManageVmAction;
import com.google.inject.Guice;
import com.google.inject.Injector;

import gdg.hfs.orchestration.CommandContext;
import gdg.hfs.orchestration.GuiceCommandProvider;

public class StartVmTest {
    VmService vmService = mock(VmService.class);
    WaitForManageVmAction waitAction = mock(WaitForManageVmAction.class);
    Long hfsVmId = 23L;

    Injector injector = Guice.createInjector(binder -> {
        binder.bind(WaitForManageVmAction.class).toInstance(waitAction);
    });
    CommandContext context = spy(new TestCommandContext(new GuiceCommandProvider(injector)));

    StartVm command = new StartVm(vmService);

    @Test
    public void testExecute() {
        VmAction hfsStartAction = mock(VmAction.class);
        when(vmService.startVm(hfsVmId)).thenReturn(hfsStartAction);

        assertNull(command.execute(context, hfsVmId));
        verify(vmService).startVm(hfsVmId);
        verify(context).execute(WaitForManageVmAction.class, hfsStartAction);
    }

}
