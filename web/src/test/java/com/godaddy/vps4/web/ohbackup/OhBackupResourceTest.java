package com.godaddy.vps4.web.ohbackup;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyLong;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import com.godaddy.hfs.config.Config;
import com.godaddy.vps4.oh.OhBackupDataService;
import com.godaddy.vps4.oh.backups.NamedOhBackup;
import com.godaddy.vps4.oh.backups.OhBackupData;
import com.godaddy.vps4.oh.backups.OhBackupService;
import com.godaddy.vps4.oh.backups.models.OhBackup;
import com.godaddy.vps4.oh.backups.models.OhBackupPurpose;
import com.godaddy.vps4.oh.backups.models.OhBackupState;
import com.godaddy.vps4.orchestration.ohbackup.Vps4DestroyOhBackup;
import com.godaddy.vps4.orchestration.ohbackup.Vps4RestoreOhBackup;
import com.godaddy.vps4.security.GDUserMock;
import com.godaddy.vps4.snapshot.SnapshotService;
import com.godaddy.vps4.snapshot.SnapshotType;
import com.godaddy.vps4.vm.Action;
import com.godaddy.vps4.vm.ActionService;
import com.godaddy.vps4.vm.ActionType;
import com.godaddy.vps4.vm.ServerSpec;
import com.godaddy.vps4.vm.ServerType;
import com.godaddy.vps4.vm.VirtualMachine;
import com.godaddy.vps4.web.Vps4Exception;
import com.godaddy.vps4.web.Vps4NoShopperException;
import com.godaddy.vps4.web.security.GDUser;
import com.godaddy.vps4.web.vm.VmResource;

import gdg.hfs.orchestration.CommandGroupSpec;
import gdg.hfs.orchestration.CommandService;
import gdg.hfs.orchestration.CommandState;

@RunWith(MockitoJUnitRunner.class)
public class OhBackupResourceTest {
    @Captor private ArgumentCaptor<CommandGroupSpec> commandCaptor;

    @Mock private ActionService actionService;
    @Mock private CommandService commandService;
    @Mock private Config config;
    @Mock private OhBackupService ohBackupService;
    @Mock private OhBackupDataService ohBackupDataService;
    @Mock private SnapshotService snapshotService;
    @Mock private VmResource vmResource;

    private OhBackup backup;
    private OhBackup hfsBackup;
    private OhBackup oldAutomaticBackup;
    private OhBackup oldFailedAutomaticBackup1;
    private OhBackup oldFailedAutomaticBackup2;
    private List<OhBackup> backups;
    private List<OhBackup> weekOfBackups;
    private List<OhBackupData> ohBackupData;
    @Mock private Action action;
    @Mock private Action conflictingAction;
    @Mock private CommandState commandState;
    @Mock private VirtualMachine vm;

    private OhBackupResource resource;
    private OhBackupResource.OhBackupRequest options;
    private GDUser user;

    @Before
    public void setUp() {
        setUpMocks();
        when(ohBackupService.getBackup(vm.vmId, backup.id)).thenReturn(backup);
        when(ohBackupService.getBackups(vm.vmId, OhBackupState.PENDING, OhBackupState.COMPLETE, OhBackupState.FAILED))
                .thenReturn(new ArrayList<>(backups));
        when(ohBackupDataService.getBackup(backup.id)).thenReturn(ohBackupData.get(0));
        when(ohBackupDataService.getBackups(vm.vmId)).thenReturn(ohBackupData);
        when(actionService.getAction(anyLong())).thenReturn(action);
        when(commandService.executeCommand(any(CommandGroupSpec.class))).thenReturn(commandState);
        when(config.get("oh.backups.enabled", "false")).thenReturn("true");
        when(config.get("vps4.autobackup.backupName")).thenReturn("auto-backup");
        when(vmResource.getVm(vm.vmId)).thenReturn(vm);
        options = new OhBackupResource.OhBackupRequest("oh-backup");
        user = GDUserMock.createShopper();
        loadResource();
    }

    private void setUpMocks() {
        backup = createBackup(daysOld(1), OhBackupState.COMPLETE, OhBackupPurpose.CUSTOMER);
        hfsBackup = createBackup(daysOld(2), OhBackupState.COMPLETE, OhBackupPurpose.CUSTOMER);
        oldAutomaticBackup = createBackup(daysOld(10), OhBackupState.COMPLETE, OhBackupPurpose.DR);
        oldFailedAutomaticBackup1 = createBackup(daysOld(8), OhBackupState.FAILED, OhBackupPurpose.DR);
        oldFailedAutomaticBackup2 = createBackup(daysOld(9), OhBackupState.FAILED, OhBackupPurpose.DR);
        ohBackupData = createBackupData(backup.id);
        conflictingAction.type = ActionType.CREATE_OH_BACKUP;
        backups = new ArrayList<>();
        weekOfBackups = new ArrayList<>();
        for (int i = 0; i < 7; i++) {
            weekOfBackups.add(createBackup(daysOld(i), OhBackupState.COMPLETE, OhBackupPurpose.DR));
        }
        backups.addAll(weekOfBackups);
        Collections.addAll(backups, backup, hfsBackup, oldAutomaticBackup,
                           oldFailedAutomaticBackup1, oldFailedAutomaticBackup2);
        commandState.commandId = UUID.randomUUID();
        vm.vmId = UUID.randomUUID();
        vm.spec = new ServerSpec();
        vm.spec.serverType = new ServerType();
        vm.spec.serverType.platform = ServerType.Platform.OPTIMIZED_HOSTING;
    }

    private Instant daysOld(int n) {
        return Instant.now().minus(n, ChronoUnit.DAYS);
    }

    private OhBackup createBackup(Instant created, OhBackupState state, OhBackupPurpose purpose) {
        OhBackup ohBackup = mock(OhBackup.class);
        ohBackup.id = UUID.randomUUID();
        ohBackup.createdAt = created;
        ohBackup.state = state;
        ohBackup.purpose = purpose;
        return ohBackup;
    }

    private List<OhBackupData> createBackupData(UUID... backupIds) {
        List<OhBackupData> obd = new ArrayList<>();
        for (UUID backupId : backupIds) {
            OhBackupData data = new OhBackupData();
            data.backupId = backupId;
            data.name = "test-backup-" + backupId;
            obd.add(data);
        }
        return obd;
    }

    private void loadResource() {
        resource = new OhBackupResource(user, vmResource, config, actionService, commandService, ohBackupService,
                                        ohBackupDataService, snapshotService);
    }

    @Test
    public void getBackups() {
        List<NamedOhBackup> result = resource.getOhBackups(vm.vmId);
        verify(vmResource).getVm(vm.vmId);
        verify(ohBackupService).getBackups(vm.vmId, OhBackupState.PENDING,
                                           OhBackupState.COMPLETE, OhBackupState.FAILED);
        assertTrue(result.stream().anyMatch(b -> b.id.equals(backup.id)));
        for (OhBackup b1 : weekOfBackups) {
            assertTrue(result.stream().anyMatch(b2 -> b2.id.equals(b1.id)));
        }
    }

    @Test
    public void getBackupsIncludesNameFromDatabase() {
        List<NamedOhBackup> result = resource.getOhBackups(vm.vmId);
        verify(ohBackupDataService).getBackups(vm.vmId);
        assertTrue(result.stream().anyMatch(b -> b.name.equals("test-backup-" + backup.id)));
        assertTrue(result.stream().anyMatch(b -> b.name.equals("auto-backup")));
    }

    @Test
    public void getBackupsRemovesHfsSnapshots() {
        List<NamedOhBackup> result = resource.getOhBackups(vm.vmId);
        assertFalse(result.stream().anyMatch(b -> b.id.equals(hfsBackup.id)));
    }

    @Test
    public void getBackupsRemovesOldAutomaticBackups() {
        List<NamedOhBackup> result = resource.getOhBackups(vm.vmId);
        for (OhBackup weekOfBackup : weekOfBackups) {
            assertTrue(result.stream().anyMatch(b -> b.id.equals(weekOfBackup.id)));
        }
        assertFalse(result.stream().anyMatch(b -> b.id.equals(oldAutomaticBackup.id)));
    }

    @Test
    public void getBackupsRemovesFailedBackups() {
        List<NamedOhBackup> result = resource.getOhBackups(vm.vmId);
        assertFalse(result.stream().anyMatch(b -> b.id.equals(oldFailedAutomaticBackup1.id)));
        assertFalse(result.stream().anyMatch(b -> b.id.equals(oldFailedAutomaticBackup2.id)));
    }

    @Test
    public void getBackupsIncludesFailedBackupIfItIsMostRecent() {
        oldFailedAutomaticBackup1.createdAt = daysOld(0);
        List<NamedOhBackup> result = resource.getOhBackups(vm.vmId);
        assertTrue(result.stream().anyMatch(b -> b.id.equals(oldFailedAutomaticBackup1.id)));
        assertFalse(result.stream().anyMatch(b -> b.id.equals(oldFailedAutomaticBackup2.id)));
    }

    @Test
    public void createBackup() {
        resource.createOhBackup(vm.vmId, options);
        verify(vmResource).getVm(vm.vmId);
        verify(actionService).createAction(eq(vm.vmId), eq(ActionType.CREATE_OH_BACKUP), anyString(), anyString());
        verify(commandService).executeCommand(any(CommandGroupSpec.class));
    }

    @Test
    public void createBackupFailsIfNameIsInvalid() {
        options = new OhBackupResource.OhBackupRequest("snapshot names can't have spaces");
        loadResource();
        try {
            resource.createOhBackup(vm.vmId, options);
            fail();
        } catch (Vps4Exception e) {
            assertEquals("INVALID_SNAPSHOT_NAME", e.getId());
        }
    }

    @Test
    public void createBackupFailsIfVmIsOpenstack() {
        vm.spec.serverType.platform = ServerType.Platform.OPENSTACK;
        loadResource();
        try {
            resource.createOhBackup(vm.vmId, options);
            fail();
        } catch (Vps4Exception e) {
            assertEquals("INVALID_PLATFORM", e.getId());
        }
    }

    @Test
    public void createBackupFailsIfConflictingActionExists() {
        when(actionService.getIncompleteActions(vm.vmId)).thenReturn(Collections.singletonList(conflictingAction));
        loadResource();
        try {
            resource.createOhBackup(vm.vmId, options);
            fail();
        } catch (Vps4Exception e) {
            assertEquals("CONFLICTING_INCOMPLETE_ACTION", e.getId());
        }
    }

    @Test
    public void hfsSnapshotsCountTowardsQuota() {
        when(ohBackupDataService.totalFilledSlots(any(UUID.class))).thenReturn(0);
        when(snapshotService.totalFilledSlots(any(UUID.class), any(SnapshotType.class))).thenReturn(2);
        try {
            resource.createOhBackup(vm.vmId, options);
            fail();
        } catch (Vps4Exception e) {
            assertEquals("SNAPSHOT_OVER_QUOTA", e.getId());
        }
    }

    @Test
    public void onDemandOhBackupsCountTowardsQuota() {
        when(ohBackupDataService.totalFilledSlots(any(UUID.class))).thenReturn(2);
        when(snapshotService.totalFilledSlots(any(UUID.class), any(SnapshotType.class))).thenReturn(0);
        try {
            resource.createOhBackup(vm.vmId, options);
            fail();
        } catch (Vps4Exception e) {
            assertEquals("SNAPSHOT_OVER_QUOTA", e.getId());
        }
    }

    @Test
    public void createBackupFailsIfAnotherBackupIsInProgress() {
        when(ohBackupService.getBackups(vm.vmId, OhBackupState.PENDING))
                .thenReturn(backups);
        try {
            resource.createOhBackup(vm.vmId, options);
            fail();
        } catch (Vps4Exception e) {
            assertEquals("SNAPSHOT_ALREADY_IN_PROGRESS", e.getId());
        }
    }

    @Test
    public void getBackup() {
        NamedOhBackup result = resource.getOhBackup(vm.vmId, backup.id);
        verify(vmResource).getVm(vm.vmId);
        verify(ohBackupService).getBackup(vm.vmId, backup.id);
        assertEquals(backup.id, result.id);
    }

    @Test
    public void getBackupIncludesNameFromDatabase() {
        NamedOhBackup result = resource.getOhBackup(vm.vmId, backup.id);
        verify(ohBackupDataService).getBackup(backup.id);
        assertEquals("test-backup-" + backup.id, result.name);
    }

    @Test
    public void destroyBackup() {
        resource.destroyOhBackup(vm.vmId, backup.id);
        verify(vmResource).getVm(vm.vmId);
        verify(ohBackupService).getBackup(vm.vmId, backup.id);
        verify(actionService).createAction(eq(vm.vmId), eq(ActionType.DESTROY_OH_BACKUP), anyString(), anyString());
        verify(commandService).executeCommand(commandCaptor.capture());
        CommandGroupSpec spec = commandCaptor.getValue();
        Vps4DestroyOhBackup.Request capturedRequest = (Vps4DestroyOhBackup.Request) spec.commands.get(0).request;
        assertSame(vm, capturedRequest.virtualMachine);
        assertEquals(backup.id, capturedRequest.backupId);
    }

    @Test
    public void destroyBackupFailsIfConflictingActionExists() {
        when(actionService.getIncompleteActions(vm.vmId)).thenReturn(Collections.singletonList(conflictingAction));
        loadResource();
        try {
            resource.destroyOhBackup(vm.vmId, backup.id);
            fail();
        } catch (Vps4Exception e) {
            assertEquals("CONFLICTING_INCOMPLETE_ACTION", e.getId());
        }
    }

    @Test
    public void restoreBackup() {
        resource.restoreOhBackup(vm.vmId, backup.id);
        verify(vmResource).getVm(vm.vmId);
        verify(ohBackupService).getBackup(vm.vmId, backup.id);
        verify(actionService).createAction(eq(vm.vmId), eq(ActionType.RESTORE_OH_BACKUP), anyString(), anyString());
        verify(commandService).executeCommand(commandCaptor.capture());
        CommandGroupSpec spec = commandCaptor.getValue();
        Vps4RestoreOhBackup.Request capturedRequest = (Vps4RestoreOhBackup.Request) spec.commands.get(0).request;
        assertSame(vm, capturedRequest.virtualMachine);
        assertEquals(backup.id, capturedRequest.backupId);
    }

    @Test
    public void restoreBackupFailsIfConflictingActionExists() {
        when(actionService.getIncompleteActions(vm.vmId)).thenReturn(Collections.singletonList(conflictingAction));
        loadResource();
        try {
            resource.restoreOhBackup(vm.vmId, backup.id);
            fail();
        } catch (Vps4Exception e) {
            assertEquals("CONFLICTING_INCOMPLETE_ACTION", e.getId());
        }
    }

    @Test
    public void restoreBackupFailsIfBackupIsDeleted() {
        backup.state = OhBackupState.DELETED;
        try {
            resource.restoreOhBackup(vm.vmId, backup.id);
            fail();
        } catch (Vps4Exception e) {
            assertEquals("INVALID_STATE", e.getId());
        }
        verify(actionService, never()).createAction(any(UUID.class), any(ActionType.class), anyString(), anyString());
        verify(commandService, never()).executeCommand(any(CommandGroupSpec.class));
    }
}
