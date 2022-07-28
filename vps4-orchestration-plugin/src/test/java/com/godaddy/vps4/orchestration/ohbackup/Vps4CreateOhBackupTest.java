package com.godaddy.vps4.orchestration.ohbackup;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
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

import com.godaddy.vps4.oh.OhBackupDataService;
import com.godaddy.vps4.oh.backups.OhBackupData;
import com.godaddy.vps4.oh.backups.OhBackupService;
import com.godaddy.vps4.oh.backups.models.OhBackup;
import com.godaddy.vps4.orchestration.snapshot.Vps4DeprecateSnapshot;
import com.godaddy.vps4.snapshot.Snapshot;
import com.godaddy.vps4.snapshot.SnapshotService;
import com.godaddy.vps4.snapshot.SnapshotStatus;
import com.godaddy.vps4.snapshot.SnapshotType;
import com.godaddy.vps4.vm.ActionService;
import com.godaddy.vps4.vm.VirtualMachine;

import gdg.hfs.orchestration.CommandContext;

@RunWith(MockitoJUnitRunner.class)
public class Vps4CreateOhBackupTest {
    @Mock private ActionService actionService;
    @Mock private CommandContext context;
    @Mock private OhBackupService ohBackupService;
    @Mock private OhBackupDataService ohBackupDataService;
    @Mock private SnapshotService snapshotService;

    @Mock private OhBackup newBackup;
    @Mock private OhBackupData oldBackupData;
    @Mock private VirtualMachine vm;
    private Snapshot oldSnapshot;

    @Captor private ArgumentCaptor<WaitForOhJob.Request> pollOhActionCaptor;
    @Captor private ArgumentCaptor<Function<CommandContext, OhBackup>> createBackupCaptor;
    @Captor private ArgumentCaptor<Function<CommandContext, Void>> cancelSnapshotsCaptor;
    @Captor private ArgumentCaptor<Function<CommandContext, UUID>> markForDeprecationCaptor;
    @Captor private ArgumentCaptor<Vps4DestroyOhBackup.Request> destroyBackupCaptor;
    @Captor private ArgumentCaptor<Vps4DeprecateSnapshot.Request> deprecateSnapshotCaptor;

    private Vps4CreateOhBackup command;
    private Vps4CreateOhBackup.Request request;

    @Before
    public void setUp() {
        setUpMocks();
        when(context.execute(eq("CreateOhBackup"),
                             Matchers.<Function<CommandContext, OhBackup>>any(),
                             eq(OhBackup.class))).thenReturn(newBackup);
        when(ohBackupService.createBackup(vm.vmId)).thenReturn(newBackup);
        when(snapshotService.markOldestSnapshotForDeprecation(vm.orionGuid, SnapshotType.ON_DEMAND))
                .thenReturn(oldSnapshot.id);
        command = new Vps4CreateOhBackup(actionService, ohBackupService, ohBackupDataService, snapshotService);
        request = new Vps4CreateOhBackup.Request(vm, "fake-employee", "oh-backup");
    }

    private void setUpMocks() {
        newBackup.id = UUID.randomUUID();
        newBackup.jobId = UUID.randomUUID();
        oldBackupData.backupId = UUID.randomUUID();
        vm.vmId = UUID.randomUUID();
        vm.orionGuid = UUID.randomUUID();
        oldSnapshot = createMockSnapshot();
    }

    private OhBackupData createMockBackupData(String created) {
        OhBackupData backupData = mock(OhBackupData.class);
        backupData.backupId = UUID.randomUUID();
        backupData.created = Instant.parse(created);
        return backupData;
    }

    private Snapshot createMockSnapshot(String created) {
        Instant createdInstant = Instant.parse(created);
        return new Snapshot(UUID.randomUUID(), 0L, vm.vmId, "backup", SnapshotStatus.LIVE,
                            createdInstant, Instant.MAX, "", 0L, SnapshotType.ON_DEMAND);
    }

    private Snapshot createMockSnapshot() {
        return new Snapshot(UUID.randomUUID(), 0L, vm.vmId, "backup", SnapshotStatus.LIVE, Instant.MIN,
                            Instant.MAX, "", 0L, SnapshotType.ON_DEMAND);
    }

    @Test
    public void cancelsErroredSnapshots() {
        command.executeWithAction(context, request);
        verify(context).execute(eq("CancelErroredSnapshots"), cancelSnapshotsCaptor.capture(), eq(Void.class));
        Function<CommandContext, Void> lambdaValue = cancelSnapshotsCaptor.getValue();
        lambdaValue.apply(context);
        verify(snapshotService).cancelErroredSnapshots(vm.orionGuid, SnapshotType.ON_DEMAND);
    }

    @Test
    public void deprecatesSnapshotIfQuotaIsReached() {
        when(snapshotService.getOldestLiveSnapshot(vm.orionGuid, SnapshotType.ON_DEMAND)).thenReturn(oldSnapshot);
        when(snapshotService.totalFilledSlots(vm.orionGuid, SnapshotType.ON_DEMAND)).thenReturn(1);
        command.executeWithAction(context, request);
        verify(context).execute(eq("MarkOldestSnapshotForDeprecation-" + vm.orionGuid),
                                markForDeprecationCaptor.capture(),
                                eq(UUID.class));
        Function<CommandContext, UUID> lambdaValue = markForDeprecationCaptor.getValue();
        UUID returnValue = lambdaValue.apply(context);
        assertSame(oldSnapshot.id, returnValue);
    }

    @Test
    public void doesNotDeprecateIfQuotaIsNotReached() {
        command.executeWithAction(context, request);
        verify(context, never()).execute(eq("MarkOldestSnapshotForDeprecation-" + vm.orionGuid),
                                         Matchers.<Function<CommandContext, UUID>>any(),
                                         eq(UUID.class));
    }

    @Test
    public void doesNotDeprecateIfOnlyOhBackupsExist() {
        when(ohBackupDataService.getOldestBackup(vm.vmId)).thenReturn(oldBackupData);
        when(ohBackupDataService.totalFilledSlots(vm.vmId)).thenReturn(1);
        command.executeWithAction(context, request);
        verify(context, never()).execute(eq("MarkOldestSnapshotForDeprecation-" + vm.orionGuid),
                                         Matchers.<Function<CommandContext, UUID>>any(),
                                         eq(UUID.class));
    }

    @Test
    public void doesNotDeprecateIfOldestIsOhBackup() {
        OhBackupData backupData = createMockBackupData("2022-01-01T00:00:00.00Z");
        Snapshot snapshot = createMockSnapshot("2022-01-02T00:00:00.00Z");
        when(ohBackupDataService.getOldestBackup(vm.vmId)).thenReturn(backupData);
        when(ohBackupDataService.totalFilledSlots(vm.vmId)).thenReturn(1);
        when(snapshotService.getOldestLiveSnapshot(vm.orionGuid, SnapshotType.ON_DEMAND)).thenReturn(snapshot);
        when(snapshotService.totalFilledSlots(vm.orionGuid, SnapshotType.ON_DEMAND)).thenReturn(1);
        command.executeWithAction(context, request);
        verify(context, never()).execute(eq("MarkOldestSnapshotForDeprecation-" + vm.orionGuid),
                                         Matchers.<Function<CommandContext, UUID>>any(),
                                         eq(UUID.class));
    }

    @Test
    public void deprecatesIfOldestIsSnapshot() {
        OhBackupData backupData = createMockBackupData("2022-01-02T00:00:00.00Z");
        Snapshot snapshot = createMockSnapshot("2022-01-01T00:00:00.00Z");
        when(ohBackupDataService.getOldestBackup(vm.vmId)).thenReturn(backupData);
        when(ohBackupDataService.totalFilledSlots(vm.vmId)).thenReturn(1);
        when(snapshotService.getOldestLiveSnapshot(vm.orionGuid, SnapshotType.ON_DEMAND)).thenReturn(snapshot);
        when(snapshotService.totalFilledSlots(vm.orionGuid, SnapshotType.ON_DEMAND)).thenReturn(1);
        command.executeWithAction(context, request);
        verify(context).execute(eq("MarkOldestSnapshotForDeprecation-" + vm.orionGuid),
                                Matchers.<Function<CommandContext, UUID>>any(),
                                eq(UUID.class));
    }

    @Test
    public void createsOhBackup() {
        command.executeWithAction(context, request);
        verify(context).execute(eq("CreateOhBackup"), createBackupCaptor.capture(), eq(OhBackup.class));
        Function<CommandContext, OhBackup> lambdaValue = createBackupCaptor.getValue();
        OhBackup returnValue = lambdaValue.apply(context);
        verify(ohBackupService).createBackup(vm.vmId);
        verify(ohBackupDataService).createBackup(newBackup.id, vm.vmId, "oh-backup");
        assertSame(newBackup, returnValue);
    }

    @Test
    public void pollsForBackupToComplete() {
        command.executeWithAction(context, request);
        verify(context).execute(eq(WaitForOhJob.class), pollOhActionCaptor.capture());
        WaitForOhJob.Request result = pollOhActionCaptor.getValue();
        assertEquals(vm.vmId, result.vmId);
        assertEquals(newBackup.jobId, result.jobId);
    }

    @Test
    public void destroysOldestBackup() {
        when(ohBackupDataService.getOldestBackup(vm.vmId)).thenReturn(oldBackupData);
        when(ohBackupDataService.totalFilledSlots(vm.vmId)).thenReturn(1);
        command.executeWithAction(context, request);
        verify(context).execute(eq(Vps4DestroyOhBackup.class), destroyBackupCaptor.capture());
        Vps4DestroyOhBackup.Request destroyRequest = destroyBackupCaptor.getValue();
        assertEquals(oldBackupData.backupId, destroyRequest.backupId);
        assertSame(vm, destroyRequest.virtualMachine);
    }

    @Test
    public void destroysOldestSnapshot() {
        when(snapshotService.getOldestLiveSnapshot(vm.orionGuid, SnapshotType.ON_DEMAND)).thenReturn(oldSnapshot);
        when(snapshotService.totalFilledSlots(vm.orionGuid, SnapshotType.ON_DEMAND)).thenReturn(1);
        command.executeWithAction(context, request);
        verify(context).execute(eq(Vps4DeprecateSnapshot.class), deprecateSnapshotCaptor.capture());
        Vps4DeprecateSnapshot.Request deprecateRequest = deprecateSnapshotCaptor.getValue();
        assertEquals(request.virtualMachine.vmId, deprecateRequest.vmId);
        assertEquals(oldSnapshot.id, deprecateRequest.snapshotIdToDeprecate);
        assertEquals(request.initiatedBy, deprecateRequest.initiatedBy);
    }
}
