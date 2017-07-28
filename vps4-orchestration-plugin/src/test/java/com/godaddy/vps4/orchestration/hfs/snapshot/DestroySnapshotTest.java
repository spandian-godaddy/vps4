package com.godaddy.vps4.orchestration.hfs.snapshot;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.Test;

import com.godaddy.vps4.orchestration.TestCommandContext;
import com.godaddy.vps4.orchestration.hfs.snapshot.DestroySnapshot;
import com.godaddy.vps4.orchestration.snapshot.WaitForSnapshotAction;
import com.google.inject.Guice;
import com.google.inject.Injector;

import gdg.hfs.orchestration.CommandContext;
import gdg.hfs.orchestration.GuiceCommandProvider;
import gdg.hfs.vhfs.snapshot.SnapshotAction;
import gdg.hfs.vhfs.snapshot.SnapshotService;

public class DestroySnapshotTest {

    SnapshotService hfsSnapshotService = mock(SnapshotService.class);
    WaitForSnapshotAction waitAction = mock(WaitForSnapshotAction.class);
    long snapshotId = 1234L;

    DestroySnapshot command = new DestroySnapshot(hfsSnapshotService);

    Injector injector = Guice.createInjector(binder -> {
        binder.bind(WaitForSnapshotAction.class).toInstance(waitAction);
    });

    CommandContext context = spy(new TestCommandContext(new GuiceCommandProvider(injector)));

    @Test
    public void testExecuteSuccess() throws Exception {

        long snapshotId = 1234L;
        SnapshotAction snapshotAction = mock(SnapshotAction.class);

        when(hfsSnapshotService.destroySnapshot(snapshotId)).thenReturn(snapshotAction);

        command.execute(context, snapshotId);

        verify(hfsSnapshotService, times(1)).destroySnapshot(snapshotId);
        verify(context, times(1)).execute(WaitForSnapshotAction.class, snapshotAction);
    }

    @Test(expected = RuntimeException.class)
    public void testDestroySnapshotFails() throws Exception {
        // if HFS throws an exception on hfsSnapshotService, the command should fail
        when(hfsSnapshotService.destroySnapshot(snapshotId)).thenThrow(new RuntimeException("Faked HFS failure"));

        command.execute(context, snapshotId);
    }

}