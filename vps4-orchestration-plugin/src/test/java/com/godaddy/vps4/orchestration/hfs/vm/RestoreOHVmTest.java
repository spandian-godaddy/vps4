package com.godaddy.vps4.orchestration.hfs.vm;

import static org.junit.Assert.assertEquals;
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

public class RestoreOHVmTest {
    VmService hfsVmService = mock(VmService.class);
    WaitForVmAction waitAction = mock(WaitForVmAction.class);
    long hfsVmId = 23L;
    long hfsSnapshotId = 56L;

    Injector injector = Guice.createInjector(binder -> {
        binder.bind(WaitForVmAction.class).toInstance(waitAction);
    });
    CommandContext context = spy(new TestCommandContext(new GuiceCommandProvider(injector)));

    RestoreOHVm command = new RestoreOHVm(hfsVmService);

    @Test
    public void testExecute() {
        VmAction hfsRestoreAction = mock(VmAction.class);
        when(hfsVmService.restore(hfsVmId, hfsSnapshotId)).thenReturn(hfsRestoreAction);

        RestoreOHVm.Request restoreOHVmRequest = new RestoreOHVm.Request(hfsVmId, hfsSnapshotId);
        assertEquals(hfsRestoreAction, command.execute(context, restoreOHVmRequest));
        verify(hfsVmService).restore(hfsVmId, hfsSnapshotId);
        verify(context).execute(WaitForVmAction.class, hfsRestoreAction);
    }

}
