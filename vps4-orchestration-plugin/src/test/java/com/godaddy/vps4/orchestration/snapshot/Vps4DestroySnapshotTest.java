package com.godaddy.vps4.orchestration.snapshot;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.UUID;

import org.junit.Test;

import com.godaddy.vps4.orchestration.TestCommandContext;
import com.godaddy.vps4.orchestration.hfs.snapshot.DestroySnapshot;
import com.godaddy.vps4.snapshot.SnapshotService;
import com.godaddy.vps4.vm.ActionService;
import com.google.inject.Guice;
import com.google.inject.Injector;

import gdg.hfs.orchestration.CommandContext;
import gdg.hfs.orchestration.GuiceCommandProvider;

public class Vps4DestroySnapshotTest {

    ActionService actionService = mock(ActionService.class);
    DestroySnapshot destroySnapshotCmd = mock(DestroySnapshot.class);
    SnapshotService snapshotService = mock(SnapshotService.class);

    Vps4DestroySnapshot command = new Vps4DestroySnapshot(actionService, snapshotService);

    Injector injector = Guice.createInjector(binder -> binder.bind(DestroySnapshot.class).toInstance(destroySnapshotCmd));

    CommandContext context = spy(new TestCommandContext(new GuiceCommandProvider(injector)));

    @Test
    public void testExecuteSuccess() {
        Vps4DestroySnapshot.Request request = new Vps4DestroySnapshot.Request();
        request.hfsSnapshotId = 42L;
        request.vps4SnapshotId = UUID.randomUUID();

        command.execute(context, request);

        verify(context, times(1)).execute("DestroySnapshot", DestroySnapshot.class, request.hfsSnapshotId);
        verify(snapshotService, times(1)).markSnapshotDestroyed(request.vps4SnapshotId);
    }

    @Test(expected = RuntimeException.class)
    public void testVps4DestroySnapshotFails() {
        Vps4DestroySnapshot.Request request = new Vps4DestroySnapshot.Request();
        request.hfsSnapshotId = 42L;

        // if HFS throws an exception on execute DestroySnapshot, the command should fail
        when(destroySnapshotCmd.execute(any(),  anyLong())).thenThrow(new RuntimeException("Faked HFS failure"));

        command.execute(context, request);
    }


}