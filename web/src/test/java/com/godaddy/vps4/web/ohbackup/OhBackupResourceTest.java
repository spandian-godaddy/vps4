package com.godaddy.vps4.web.ohbackup;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.fail;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyLong;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
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
import com.godaddy.vps4.vm.VirtualMachine;
import com.godaddy.vps4.vm.VmAction;
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

    @Mock private Action action;
    @Mock private OhBackup backup;
    @Mock private List<OhBackup> backups;
    @Mock private CommandState commandState;
    @Mock private VirtualMachine vm;

    private OhBackupResource resource;
    private GDUser user;

    @Before
    public void setUp() {
        setUpMocks();
        when(ohBackupService.getBackup(vm.vmId, backup.id)).thenReturn(backup);
        when(ohBackupService.getBackups(vm.vmId, OhBackupState.PENDING, OhBackupState.COMPLETE, OhBackupState.FAILED))
                .thenReturn(backups);
        when(actionService.getAction(anyLong())).thenReturn(action);
        when(commandService.executeCommand(any(CommandGroupSpec.class))).thenReturn(commandState);
        when(config.get("oh.backups.enabled", "false")).thenReturn("true");
        when(vmResource.getVm(vm.vmId)).thenReturn(vm);
        user = GDUserMock.createShopper();
        loadResource();
    }

    private void setUpMocks() {
        backup = createBackup(OhBackupState.COMPLETE, OhBackupPurpose.CUSTOMER);
        backups = new ArrayList<>();
        backups.add(backup);
        commandState.commandId = UUID.randomUUID();
        vm.vmId = UUID.randomUUID();
    }

    private OhBackup createBackup(OhBackupState state, OhBackupPurpose purpose) {
        OhBackup ohBackup = mock(OhBackup.class);
        ohBackup.jobId = UUID.randomUUID();
        ohBackup.state = state;
        ohBackup.purpose = purpose;
        return ohBackup;
    }

    private void loadResource() {
        resource = new OhBackupResource(user, vmResource, config, actionService, commandService, ohBackupService,
                                        ohBackupDataService, snapshotService);
    }

    @Test
    public void getBackups() {
        List<OhBackup> result = resource.getOhBackups(vm.vmId);
        verify(vmResource).getVm(vm.vmId);
        verify(ohBackupService).getBackups(vm.vmId, OhBackupState.PENDING,
                                           OhBackupState.COMPLETE, OhBackupState.FAILED);
        for (int i = 0; i < backups.size(); i++) {
            assertSame(backups.get(i), result.get(i));
        }
    }

    @Test
    public void createBackup() {
        resource.createOhBackup(vm.vmId);
        verify(vmResource).getVm(vm.vmId);
        verify(actionService).createAction(eq(vm.vmId), eq(ActionType.CREATE_OH_BACKUP), anyString(), anyString());
        verify(commandService).executeCommand(any(CommandGroupSpec.class));
    }

    @Test
    public void createBackupFailsIfUserIsNotShopper() {
        user = GDUserMock.createAdmin();
        loadResource();
        try {
            resource.createOhBackup(vm.vmId);
            fail();
        } catch (Vps4NoShopperException e) {
            assertEquals("SHOPPER_ID_REQUIRED", e.getId());
        }
    }

    @Test
    public void hfsSnapshotsCountTowardsQuota() {
        when(ohBackupDataService.totalFilledSlots(any(UUID.class))).thenReturn(0);
        when(snapshotService.totalFilledSlots(any(UUID.class), any(SnapshotType.class))).thenReturn(2);
        try {
            resource.createOhBackup(vm.vmId);
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
            resource.createOhBackup(vm.vmId);
            fail();
        } catch (Vps4Exception e) {
            assertEquals("SNAPSHOT_OVER_QUOTA", e.getId());
        }
    }

    @Test
    public void automaticOhBackupsDoNotCountTowardsQuota() {
        List<OhBackup> largeBackupList = new ArrayList<>();
        largeBackupList.add(createBackup(OhBackupState.COMPLETE, OhBackupPurpose.DR));
        largeBackupList.add(createBackup(OhBackupState.COMPLETE, OhBackupPurpose.DR));
        when(ohBackupService.getBackups(vm.vmId, OhBackupState.PENDING, OhBackupState.COMPLETE))
                .thenReturn(largeBackupList);
        resource.createOhBackup(vm.vmId);
    }

    @Test
    public void createBackupFailsIfAnotherBackupIsInProgress() {
        when(ohBackupService.getBackups(vm.vmId, OhBackupState.PENDING))
                .thenReturn(backups);
        try {
            resource.createOhBackup(vm.vmId);
            fail();
        } catch (Vps4Exception e) {
            assertEquals("SNAPSHOT_ALREADY_IN_PROGRESS", e.getId());
        }
    }

    @Test
    public void getBackup() {
        OhBackup result = resource.getOhBackup(vm.vmId, backup.id);
        verify(vmResource).getVm(vm.vmId);
        verify(ohBackupService).getBackup(vm.vmId, backup.id);
        assertSame(backup, result);
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
}
