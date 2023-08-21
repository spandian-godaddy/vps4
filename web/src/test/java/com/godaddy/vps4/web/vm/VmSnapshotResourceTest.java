package com.godaddy.vps4.web.vm;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyLong;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.UUID;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import com.godaddy.hfs.config.Config;
import com.godaddy.hfs.vm.Extended;
import com.godaddy.hfs.vm.Vm;
import com.godaddy.hfs.vm.VmExtendedInfo;
import com.godaddy.hfs.vm.VmService;
import com.godaddy.vps4.oh.OhBackupDataService;
import com.godaddy.vps4.oh.backups.OhBackupService;
import com.godaddy.vps4.scheduler.api.core.SchedulerJobDetail;
import com.godaddy.vps4.scheduler.api.web.SchedulerWebService;
import com.godaddy.vps4.security.GDUserMock;
import com.godaddy.vps4.snapshot.Snapshot;
import com.godaddy.vps4.snapshot.SnapshotAction;
import com.godaddy.vps4.snapshot.SnapshotService;
import com.godaddy.vps4.snapshot.SnapshotStatus;
import com.godaddy.vps4.snapshot.SnapshotType;
import com.godaddy.vps4.util.TroubleshootVmService;
import com.godaddy.vps4.vm.Action;
import com.godaddy.vps4.vm.ActionService;
import com.godaddy.vps4.vm.ActionStatus;
import com.godaddy.vps4.vm.ActionType;
import com.godaddy.vps4.vm.ServerSpec;
import com.godaddy.vps4.vm.ServerType;
import com.godaddy.vps4.vm.VirtualMachine;
import com.godaddy.vps4.web.Vps4Exception;
import com.godaddy.vps4.web.security.GDUser;
import com.google.inject.Provider;

import gdg.hfs.orchestration.CommandGroupSpec;
import gdg.hfs.orchestration.CommandService;
import gdg.hfs.orchestration.CommandState;

import javax.ws.rs.ForbiddenException;

@RunWith(MockitoJUnitRunner.class)
public class VmSnapshotResourceTest {
    @Mock private VmResource vmResource;
    @Mock private Provider<VmSnapshotActionResource> actionResourceProvider;
    @Mock private VmSnapshotActionResource vmSnapshotActionResource;
    @Mock private ActionService actionService;
    @Mock private CommandService commandService;
    @Mock private OhBackupService ohBackupService;
    @Mock private OhBackupDataService ohBackupDataService;
    @Mock private SchedulerWebService schedulerWebService;
    @Mock private SnapshotService snapshotService;
    @Mock private TroubleshootVmService troubleshootVmService;
    @Mock private VmService vmService;
    @Mock private Config config;

    private Action renameAction;
    private List<Action> incompleteActions;
    private GDUser user;
    private SchedulerJobDetail backupJob;
    private Snapshot snapshot;
    private Snapshot destroyedSnapshot;
    private List<Snapshot> snapshots;
    private VirtualMachine vm;
    private Vm hfsVm;
    private VmExtendedInfo extendedInfo;
    private VmSnapshotResource resource;


    @Before
    public void setup() {
        user = GDUserMock.createShopper();

        setupResource();
        setupVm();
        setupActions();
        setupBackupJob(false);
        setupHfsVm();
        setupExtendedInfo();
        setupSnapshots();

        when(actionResourceProvider.get()).thenReturn(vmSnapshotActionResource);
        when(actionService.createAction(eq(snapshot.id), eq(ActionType.RENAME_SNAPSHOT), anyString(), anyString()))
                .thenReturn(renameAction.id);
        when(actionService.getAction(anyLong()))
                .thenReturn(createTestAction(ActionType.CREATE_SNAPSHOT, ActionStatus.COMPLETE));
        when(actionService.getAction(renameAction.id)).thenReturn(renameAction);
        when(actionService.getIncompleteActions(snapshot.id)).thenReturn(incompleteActions);
        when(commandService.executeCommand(any())).thenReturn(createTestCommand());
        when(config.get("oh.backups.enabled", "false")).thenReturn("true");
        when(config.get("vps4.autobackup.checkHvConcurrentLimit")).thenReturn("true");
        when(config.get("vps4.autobackup.concurrentLimit","50")).thenReturn("50");
        when(config.get("vps4.snapshot.currentlyPaused")).thenReturn("false");
        when(snapshotService.getSnapshot(snapshot.id)).thenReturn(snapshot);
        when(snapshotService.getSnapshot(destroyedSnapshot.id)).thenReturn(destroyedSnapshot);
        when(snapshotService.getSnapshotsForVm(vm.vmId)).thenReturn(snapshots);
        when(snapshotService.getVmIdWithInProgressSnapshotOnHv(extendedInfo.extended.hypervisorHostname))
                .thenReturn(null);
        when(snapshotService.totalSnapshotsInProgress()).thenReturn(40);
        when(troubleshootVmService.getHfsAgentStatus(hfsVm.vmId)).thenReturn("OK");
        when(vmService.getVm(vm.hfsVmId)).thenReturn(hfsVm);
        when(vmService.getVmExtendedInfo(vm.hfsVmId)).thenReturn(extendedInfo);
        when(vmResource.getVm(vm.vmId)).thenReturn(vm);
    }

    private void setupResource() {
        resource = new VmSnapshotResource(user, vmResource, actionResourceProvider, actionService, commandService,
                                          ohBackupService, ohBackupDataService, schedulerWebService, snapshotService,
                                          troubleshootVmService, vmService, config);
    }

    private void setupVm() {
        vm = new VirtualMachine();
        vm.hfsVmId = 40;
        vm.spec = new ServerSpec();
        vm.spec.serverType = new ServerType();
        vm.spec.serverType.platform = ServerType.Platform.OPENSTACK;
        vm.vmId = UUID.randomUUID();
    }

    private void setupBackupJob(boolean isPaused) {
        backupJob = new SchedulerJobDetail(UUID.randomUUID(), Instant.MAX, null, isPaused);
        when(schedulerWebService.getJob(anyString(), anyString(), eq(vm.backupJobId))).thenReturn(backupJob);
    }

    private void setupHfsVm() {
        hfsVm = new Vm();
        hfsVm.vmId = vm.hfsVmId;
        hfsVm.status = "ACTIVE";
    }

    private void setupExtendedInfo() {
        extendedInfo = new VmExtendedInfo();
        extendedInfo.extended = new Extended();
        extendedInfo.extended.hypervisorHostname = "hv.test.gdg";
    }

    private void setupSnapshots() {
        snapshot = createTestSnapshot(SnapshotStatus.LIVE);
        destroyedSnapshot = createTestSnapshot(SnapshotStatus.DESTROYED);
        snapshots = new ArrayList<>();
        snapshots.add(createTestSnapshot(SnapshotStatus.LIVE));
        snapshots.add(createTestSnapshot(SnapshotStatus.IN_PROGRESS));
        snapshots.add(createTestSnapshot(SnapshotStatus.ERROR));
        snapshots.add(createTestSnapshot(SnapshotStatus.DESTROYED));
        snapshots.add(createTestSnapshot(SnapshotStatus.CANCELLED));
    }

    private void setupActions() {
        renameAction = createTestAction(ActionType.RENAME_SNAPSHOT, ActionStatus.COMPLETE);
        incompleteActions = new ArrayList<>();
        incompleteActions.add(createTestAction(ActionType.DESTROY_SNAPSHOT, ActionStatus.IN_PROGRESS));
    }

    private Action createTestAction(ActionType type, ActionStatus status) {
        long id = new Random().nextLong();
        return new Action(id, UUID.randomUUID(), type, null, null, null, status,
                          null, null, null, null, null);
    }

    private CommandState createTestCommand() {
        return new CommandState();
    }

    private Snapshot createTestSnapshot(SnapshotStatus status) {
        return new Snapshot(UUID.randomUUID(), 0L, vm.vmId, "backup", status, Instant.MIN, Instant.MAX,
                            "", 0L, SnapshotType.AUTOMATIC);
    }

    // GET /{vmId}/snapshots

    @Test
    public void getsHfsSnapshotsIfOhFlagIsDisabled() {
        resource.getSnapshotsForVM(vm.vmId);
        verify(snapshotService).getSnapshotsForVm(vm.vmId);
    }

    @Test
    public void filtersDestroyedSnapshots() {
        List<Snapshot> result = resource.getSnapshotsForVM(vm.vmId);
        assertTrue(result.stream().anyMatch(s -> s.status == SnapshotStatus.LIVE));
        assertTrue(result.stream().anyMatch(s -> s.status == SnapshotStatus.IN_PROGRESS));
        assertTrue(result.stream().anyMatch(s -> s.status == SnapshotStatus.ERROR));
        assertTrue(result.stream().noneMatch(s -> s.status == SnapshotStatus.DESTROYED));
        assertTrue(result.stream().noneMatch(s -> s.status == SnapshotStatus.CANCELLED));
    }

    // POST /{vmId}/snapshots

    private VmSnapshotResource.VmSnapshotRequest createSnapshotRequest() {
        VmSnapshotResource.VmSnapshotRequest request = new VmSnapshotResource.VmSnapshotRequest();
        request.name = "backup";
        request.snapshotType = SnapshotType.AUTOMATIC;
        return request;
    }

    @Test
    public void createsHfsSnapshot() {
        VmSnapshotResource.VmSnapshotRequest request = createSnapshotRequest();
        resource.createSnapshot(vm.vmId, request);
        verify(vmResource).getVm(vm.vmId);
        verify(snapshotService).createSnapshot(vm.projectId, vm.vmId, request.name, request.snapshotType);
        verify(actionService).createAction(any(UUID.class), eq(ActionType.CREATE_SNAPSHOT), anyString(), anyString());
        ArgumentCaptor<CommandGroupSpec> captor = ArgumentCaptor.forClass(CommandGroupSpec.class);
        verify(commandService).executeCommand(captor.capture());
        assertEquals("Vps4SnapshotVm", captor.getValue().commands.get(0).command);
    }

    @Test
    public void abortsIfPausedInConfig() {
        VmSnapshotResource.VmSnapshotRequest request = createSnapshotRequest();
        when(config.get("vps4.snapshot.currentlyPaused")).thenReturn("true");
        try {
            resource.createSnapshot(vm.vmId, request);
        } catch (Vps4Exception e) {
            assertEquals("JOB_PAUSED", e.getId());
        }
        verify(snapshotService, never())
                .createSnapshot(anyLong(), any(UUID.class), anyString(), any(SnapshotType.class));
    }

    @Test(expected = ForbiddenException.class)
    public void abortsIfAuthCheckFails() {
        VmSnapshotResource.VmSnapshotRequest request = createSnapshotRequest();
        when(vmResource.getVm(vm.vmId)).thenThrow(new ForbiddenException("test"));
        resource.createSnapshot(vm.vmId, request);
    }

    @Test
    public void abortsIfSnapshotOverQuota() {
        VmSnapshotResource.VmSnapshotRequest request = createSnapshotRequest();
        when(snapshotService.totalFilledSlots(vm.orionGuid, request.snapshotType)).thenReturn(2);
        try {
            resource.createSnapshot(vm.vmId, request);
            fail();
        } catch (Vps4Exception e) {
            assertEquals("SNAPSHOT_OVER_QUOTA", e.getId());
        }
    }

    @Test
    public void abortsIfAnotherSnapshotInProgress() {
        VmSnapshotResource.VmSnapshotRequest request = createSnapshotRequest();
        when(snapshotService.hasSnapshotInProgress(vm.orionGuid)).thenReturn(true);
        try {
            resource.createSnapshot(vm.vmId, request);
            fail();
        } catch (Vps4Exception e) {
            assertEquals("SNAPSHOT_ALREADY_IN_PROGRESS", e.getId());
        }
    }

    @Test
    public void abortsIfInvalidSnapshotName() {
        VmSnapshotResource.VmSnapshotRequest request = createSnapshotRequest();
        request.name = "this name is invalid because it is really long";
        try {
            resource.createSnapshot(vm.vmId, request);
            fail();
        } catch (Vps4Exception e) {
            assertEquals("INVALID_SNAPSHOT_NAME", e.getId());
        }
    }

    @Test
    public void abortsIfSnapshotsPaused() {
        VmSnapshotResource.VmSnapshotRequest request = createSnapshotRequest();
        setupBackupJob(true);
        try {
            resource.createSnapshot(vm.vmId, request);
            fail();
        } catch (Vps4Exception e) {
            assertEquals("AUTOMATIC_SNAPSHOTS_PAUSED", e.getId());
        }
    }

    @Test
    public void abortsIfOverDcLimit() {
        VmSnapshotResource.VmSnapshotRequest request = createSnapshotRequest();
        when(snapshotService.totalSnapshotsInProgress()).thenReturn(50);
        try {
            resource.createSnapshot(vm.vmId, request);
            fail();
        } catch (Vps4Exception e) {
            assertEquals("SNAPSHOT_DC_LIMIT_REACHED", e.getId());
        }
    }

    @Test
    public void abortsIfOverHvLimit() {
        VmSnapshotResource.VmSnapshotRequest request = createSnapshotRequest();
        when(snapshotService.getVmIdWithInProgressSnapshotOnHv(extendedInfo.extended.hypervisorHostname))
                .thenReturn(UUID.randomUUID());
        try {
            resource.createSnapshot(vm.vmId, request);
            fail();
        } catch (Vps4Exception e) {
            assertEquals("SNAPSHOT_HV_LIMIT_REACHED", e.getId());
        }
    }

    @Test
    public void abortsIfNydusIsDown() {
        VmSnapshotResource.VmSnapshotRequest request = createSnapshotRequest();
        when(troubleshootVmService.getHfsAgentStatus(vm.hfsVmId)).thenReturn("UNKNOWN");
        try {
            resource.createSnapshot(vm.vmId, request);
            fail();
        } catch (Vps4Exception e) {
            assertEquals("AGENT_DOWN", e.getId());
        }
    }

    // GET /{vmId}/snapshots/{snapshotId}

    @Test
    public void getsSnapshot() {
        Snapshot result = resource.getSnapshot(vm.vmId, snapshot.id);
        verify(vmResource).getVm(snapshot.vmId);
        assertSame(snapshot, result);
    }

    @Test
    public void getsDestroyedSnapshot() {
        try {
            resource.getSnapshot(vm.vmId, destroyedSnapshot.id);
            fail();
        } catch (Vps4Exception e) {
            assertEquals("SNAPSHOT_DELETED", e.getId());
        }
    }

    @Test
    public void getsDestroyedSnapshotAsAdmin() {
        user = GDUserMock.createAdmin();
        setupResource();
        Snapshot result = resource.getSnapshot(vm.vmId, destroyedSnapshot.id);
        verify(vmResource).getVm(snapshot.vmId);
        assertSame(destroyedSnapshot, result);
    }

    // DELETE /{vmId}/snapshots/{snapshotId}

    @Test
    public void deletesSnapshot() {
        resource.destroySnapshot(vm.vmId, snapshot.id);
        verify(vmResource).getVm(snapshot.vmId);
        verify(snapshotService).deleteVmHvForSnapshotTracking(vm.vmId);
        verify(actionService).createAction(eq(snapshot.id), eq(ActionType.DESTROY_SNAPSHOT), anyString(), anyString());
        ArgumentCaptor<CommandGroupSpec> captor = ArgumentCaptor.forClass(CommandGroupSpec.class);
        verify(commandService).executeCommand(captor.capture());
        assertEquals("Vps4DestroySnapshot", captor.getValue().commands.get(0).command);
    }

    @Test
    public void cancelsIncompleteActions() {
        resource.destroySnapshot(vm.vmId, snapshot.id);
        verify(actionService).getIncompleteActions(snapshot.id);
        for (Action action : incompleteActions) {
            vmSnapshotActionResource.cancelSnapshotAction(snapshot.vmId, snapshot.id, action.id);
        }
    }

    // PATCH /{vmId}/snapshots/{snapshotId}

    private VmSnapshotResource.SnapshotRenameRequest createSnapshotRenameRequest() {
        VmSnapshotResource.SnapshotRenameRequest request = new VmSnapshotResource.SnapshotRenameRequest();
        request.name = "backup";
        return request;
    }

    @Test
    public void failsIfNameIsInvalid() {
        VmSnapshotResource.SnapshotRenameRequest request = createSnapshotRenameRequest();
        SnapshotAction result = resource.renameSnapshot(vm.vmId, snapshot.id, request);
        verify(vmResource).getVm(snapshot.vmId);
        verify(snapshotService).getSnapshot(snapshot.id);
        verify(actionService).createAction(eq(snapshot.id), eq(ActionType.RENAME_SNAPSHOT), anyString(), anyString());
        verify(snapshotService).renameSnapshot(snapshot.id, request.name);
        verify(actionService).completeAction(eq(renameAction.id), anyString(), anyString());
        assertEquals(result.id, renameAction.id);
    }

    @Test
    public void renamesSnapshot() {
        VmSnapshotResource.SnapshotRenameRequest request = createSnapshotRenameRequest();
        request.name = "this name is invalid because it is really long";
        try {
            resource.renameSnapshot(vm.vmId, snapshot.id, request);
            fail();
        } catch (Vps4Exception e) {
            assertEquals("INVALID_SNAPSHOT_NAME", e.getId());
        }
    }
}
