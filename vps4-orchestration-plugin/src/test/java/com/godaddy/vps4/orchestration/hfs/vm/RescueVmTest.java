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
import com.google.inject.Guice;
import com.google.inject.Injector;

import gdg.hfs.orchestration.CommandContext;
import gdg.hfs.orchestration.GuiceCommandProvider;

public class RescueVmTest {
    VmService vmService = mock(VmService.class);
    WaitForVmAction waitAction = mock(WaitForVmAction.class);
    Long hfsVmId = 23L;

    Injector injector = Guice.createInjector(binder -> {
        binder.bind(WaitForVmAction.class).toInstance(waitAction);
    });
    CommandContext context = spy(new TestCommandContext(new GuiceCommandProvider(injector)));

    RescueVm command = new RescueVm(vmService);

    @Test
    public void testExecute() {
        VmAction hfsRescueAction = mock(VmAction.class);
        when(vmService.rescueVm(hfsVmId)).thenReturn(hfsRescueAction);

        assertNull(command.execute(context, hfsVmId));
        verify(vmService).rescueVm(hfsVmId);
        verify(context).execute(WaitForVmAction.class, hfsRescueAction);
    }

}
