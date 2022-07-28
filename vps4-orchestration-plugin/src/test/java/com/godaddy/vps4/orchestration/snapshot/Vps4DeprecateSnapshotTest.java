package com.godaddy.vps4.orchestration.snapshot;

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.Random;
import java.util.UUID;
import java.util.function.Function;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Matchers;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import com.godaddy.vps4.snapshot.Snapshot;
import com.godaddy.vps4.snapshot.SnapshotActionService;
import com.godaddy.vps4.snapshot.SnapshotService;
import com.godaddy.vps4.snapshot.SnapshotStatus;
import com.godaddy.vps4.snapshot.SnapshotType;
import com.godaddy.vps4.vm.ActionService;
import com.godaddy.vps4.vm.ActionType;
import com.godaddy.vps4.vm.VirtualMachine;

import gdg.hfs.orchestration.CommandContext;

@RunWith(MockitoJUnitRunner.class)
public class Vps4DeprecateSnapshotTest {
    @Mock private @SnapshotActionService ActionService actionService;
    @Mock private CommandContext context;
    @Mock private SnapshotService snapshotService;

    @Mock private VirtualMachine vm;
    private Snapshot snapshot;

    @Captor private ArgumentCaptor<Function<CommandContext, Void>> markDeprecatedCaptor;
    @Captor private ArgumentCaptor<Vps4DestroySnapshot.Request> destroySnapshotCaptor;

    private Vps4DeprecateSnapshot command;
    private Vps4DeprecateSnapshot.Request request;

    @Before
    public void setUp() {
        setUpMocks();
        when(snapshotService.getSnapshot(snapshot.id)).thenReturn(snapshot);
        command = new Vps4DeprecateSnapshot(actionService, snapshotService);
        request = new Vps4DeprecateSnapshot.Request(vm.vmId, snapshot.id, "fake-employee");
    }

    private void setUpMocks() {
        long hfsId = new Random().nextLong();
        snapshot = new Snapshot(UUID.randomUUID(), 0L, vm.vmId, "backup", SnapshotStatus.LIVE,
                                Instant.now(), Instant.MAX, "", hfsId, SnapshotType.ON_DEMAND);
        vm.vmId = UUID.randomUUID();
    }

    @Test
    public void deprecatesOldSnapshot() {
        command.executeWithAction(context, request);
        verify(context).execute(eq("MarkSnapshotAsDeprecated-" + snapshot.id),
                                markDeprecatedCaptor.capture(),
                                eq(Void.class));
        Function<CommandContext, Void> lambdaValue = markDeprecatedCaptor.getValue();
        lambdaValue.apply(context);
        verify(snapshotService).updateSnapshotStatus(snapshot.id, SnapshotStatus.DEPRECATED);
    }

    @Test
    public void destroysOldSnapshot() {
        command.executeWithAction(context, request);
        verify(snapshotService).getSnapshot(snapshot.id);
        verify(actionService).createAction(eq(snapshot.id), eq(ActionType.DESTROY_SNAPSHOT),
                                           anyString(), eq(request.initiatedBy));
        verify(context).execute(eq(Vps4DestroySnapshot.class), destroySnapshotCaptor.capture());
        Vps4DestroySnapshot.Request destroyRequest = destroySnapshotCaptor.getValue();
        assertEquals(snapshot.hfsSnapshotId, (Long) destroyRequest.hfsSnapshotId);
        assertEquals(snapshot.id, destroyRequest.vps4SnapshotId);
        assertEquals(vm.vmId, destroyRequest.vmId);
    }

    @Test
    public void suppressesExceptionIfSnapshotDestroyFails() {
        when(context.execute(eq(Vps4DestroySnapshot.class), any(Vps4DestroySnapshot.Request.class)))
                .thenThrow(new RuntimeException("Error"));
        command.executeWithAction(context, request);
    }

    @Test
    public void doesNothingIfRequestIsNull() {
        request.snapshotIdToDeprecate = null;
        command.executeWithAction(context, request);
        verify(actionService, never()).createAction(any(UUID.class), any(ActionType.class), anyString(), anyString());
        verify(context, never()).execute(any(), any(Vps4DestroySnapshot.Request.class));
        verify(context, never()).execute(anyString(), Matchers.<Function<CommandContext, Void>>any(), any());
        verify(snapshotService, never()).getSnapshot(snapshot.id);
    }
}
