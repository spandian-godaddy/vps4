package com.godaddy.vps4.orchestration.hfs.snapshot;

import static org.mockito.Matchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.Test;

import com.godaddy.vps4.orchestration.TestCommandContext;
import com.godaddy.vps4.orchestration.snapshot.WaitForSnapshotAction;
import com.google.inject.Guice;
import com.google.inject.Injector;

import gdg.hfs.orchestration.CommandContext;
import gdg.hfs.orchestration.GuiceCommandProvider;
import gdg.hfs.vhfs.snapshot.Snapshot;
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
    public void testDoNotDestroySnapshotIfAlreadyDestroyed() throws Exception {
        long snapshotId = 1234L;
        Snapshot hfsSnapshot = mock(Snapshot.class);
        hfsSnapshot.destroyDate = "2019-08-16 11:31:09.504247";
        when(hfsSnapshotService.getSnapshot(snapshotId)).thenReturn(hfsSnapshot);
        command.execute(context, snapshotId);
        verify(hfsSnapshotService, never()).destroySnapshot(snapshotId);
    }

    @Test
    public void testDoNotDestroySnapshotIfNeverCompleted() throws Exception {
        long snapshotId = 1234L;
        Snapshot hfsSnapshot = mock(Snapshot.class);
        hfsSnapshot.destroyDate = null;
        hfsSnapshot.completeDate = null;
        when(hfsSnapshotService.getSnapshot(snapshotId)).thenReturn(hfsSnapshot);
        command.execute(context, snapshotId);
        verify(hfsSnapshotService, never()).destroySnapshot(snapshotId);
    }

    @Test
    public void testDoNotDestroySnapshotIfHfsIdNull() throws Exception {
        Long snapshotId = null;
        command.execute(context, snapshotId);
        verify(hfsSnapshotService, never()).getSnapshot(anyLong());
        verify(hfsSnapshotService, never()).destroySnapshot(anyLong());
    }

    @Test
    public void testDestroySnapshotSuccess() throws Exception {
        long snapshotId = 1234L;

        Snapshot hfsSnapshot = mock(Snapshot.class);
        hfsSnapshot.destroyDate = null;
        hfsSnapshot.completeDate = "2019-08-16 11:31:09.504247";
        when(hfsSnapshotService.getSnapshot(snapshotId)).thenReturn(hfsSnapshot);

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