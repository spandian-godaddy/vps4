package com.godaddy.vps4.web.backupstorage;

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyLong;
import static org.mockito.Matchers.argThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.Collections;
import java.util.UUID;

import javax.ws.rs.NotFoundException;

import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentMatcher;

import com.godaddy.hfs.backupstorage.BackupStorage;
import com.godaddy.hfs.backupstorage.BackupStorageCreds;
import com.godaddy.hfs.vm.VmService;
import com.godaddy.vps4.backupstorage.BackupStorageService;
import com.godaddy.vps4.backupstorage.jdbc.BackupStorageModel;
import com.godaddy.vps4.jdbc.ResultSubset;
import com.godaddy.vps4.vm.Action;
import com.godaddy.vps4.vm.ActionService;
import com.godaddy.vps4.vm.ActionStatus;
import com.godaddy.vps4.vm.ActionType;
import com.godaddy.vps4.vm.ServerSpec;
import com.godaddy.vps4.vm.VirtualMachine;
import com.godaddy.vps4.web.Vps4Exception;
import com.godaddy.vps4.web.security.GDUser;
import com.godaddy.vps4.web.vm.VmResource;

import gdg.hfs.orchestration.CommandGroupSpec;
import gdg.hfs.orchestration.CommandService;
import gdg.hfs.orchestration.CommandState;

public class BackupStorageResourceTest {
    GDUser user = mock(GDUser.class);
    private ActionService actionService = mock(ActionService.class);
    private BackupStorageService backupStorageService = mock(BackupStorageService.class);
    private CommandService commandService = mock(CommandService.class);
    private VmResource vmResource = mock(VmResource.class);
    private VmService vmService = mock(VmService.class);

    private VirtualMachine vm;
    private UUID vmId = UUID.randomUUID();
    private UUID orionGuid = UUID.randomUUID();
    private long hfsVmId = 42;
    private BackupStorage backupStorage = mock(BackupStorage.class);
    private BackupStorageCreds backupStorageCreds = mock(BackupStorageCreds.class);
    private BackupStorageResource resource;

    @Before
    public void setUp() {
        vm = mock(VirtualMachine.class);
        vm.orionGuid = orionGuid;
        vm.vmId = vmId;
        vm.hfsVmId = hfsVmId;
        when(vmResource.getVm(vmId)).thenReturn(vm);

        when(vmService.getBackupStorage(hfsVmId)).thenReturn(backupStorage);
        when(vmService.getBackupStorageCreds(hfsVmId)).thenReturn(backupStorageCreds);

        Action action = mock(Action.class);
        when(actionService.getAction(anyLong())).thenReturn(action);

        vm.spec = mock(ServerSpec.class);
        when (vm.spec.isVirtualMachine()).thenReturn(false);

        when(commandService.executeCommand(any())).thenReturn(new CommandState());
        resource = new BackupStorageResource(user, actionService, backupStorageService, commandService, vmResource, vmService);

        BackupStorageModel backup = new BackupStorageModel(
                12, vmId, "example.com", "ftp", Instant.EPOCH, Instant.MAX
        );
        when(backupStorageService.getBackupStorage(vmId)).thenReturn(backup);
    }

    @Test
    public void testCreateBackupStorage() {
        resource.createBackupStorage(vmId);
        verify(vmResource, times(1)).getVm(vmId);
        verify(actionService, times(1)).createAction(vmId, ActionType.CREATE_BACKUP_STORAGE,
                                                     "{}", user.getUsername());
        ArgumentMatcher<CommandGroupSpec> filter = new ArgumentMatcher<CommandGroupSpec>() {
            @Override
            public boolean matches(Object item) {
                CommandGroupSpec spec = (CommandGroupSpec) item;
                return spec.commands.get(0).command.equals("Vps4CreateBackupStorage");
            }
        };
        verify(commandService, times(1)).executeCommand(argThat(filter));
    }

    @Test
    public void testDoubleCreate() {
        Action action = mock(Action.class);
        action.type = ActionType.CREATE_BACKUP_STORAGE;
        action.status = ActionStatus.NEW;
        ResultSubset<Action> currentActions = new ResultSubset<>(Collections.singletonList(action), 1);
        when(actionService.getActionList(any())).thenReturn(currentActions);
        try {
            resource.createBackupStorage(vmId);
        } catch (Vps4Exception e) {
            assertEquals("CONFLICTING_INCOMPLETE_ACTION", e.getId());
        }
        verify(actionService, never()).createAction(vmId, ActionType.CREATE_BACKUP_STORAGE,
                                                    "{}", user.getUsername());
    }

    @Test
    public void testCreateOnVirtualServer() {
        when (vm.spec.isVirtualMachine()).thenReturn(true);
        try {
            resource.createBackupStorage(vmId);
        } catch (Vps4Exception e) {
            assertEquals("INVALID_SERVER", e.getId());
        }
        verify(actionService, never()).createAction(vmId, ActionType.CREATE_BACKUP_STORAGE,
                                                    "{}", user.getUsername());
    }

    @Test
    public void testDestroyBackupStorage() {
        resource.destroyBackupStorage(vmId);
        verify(vmResource, times(1)).getVm(vmId);
        verify(actionService, times(1)).createAction(vmId, ActionType.DESTROY_BACKUP_STORAGE,
                                                     "{}", user.getUsername());
        ArgumentMatcher<CommandGroupSpec> filter = new ArgumentMatcher<CommandGroupSpec>() {
            @Override
            public boolean matches(Object item) {
                CommandGroupSpec spec = (CommandGroupSpec) item;
                return spec.commands.get(0).command.equals("Vps4DestroyBackupStorage");
            }
        };
        verify(commandService, times(1)).executeCommand(argThat(filter));
    }

    @Test
    public void testDoubleDestroy() {
        Action action = mock(Action.class);
        action.type = ActionType.DESTROY_BACKUP_STORAGE;
        action.status = ActionStatus.NEW;
        ResultSubset<Action> currentActions = new ResultSubset<>(Collections.singletonList(action), 1);
        when(actionService.getActionList(any())).thenReturn(currentActions);
        try {
            resource.destroyBackupStorage(vmId);
        } catch (Vps4Exception e) {
            assertEquals("CONFLICTING_INCOMPLETE_ACTION", e.getId());
        }
        verify(actionService, never()).createAction(vmId, ActionType.DESTROY_BACKUP_STORAGE,
                                                    "{}", user.getUsername());
    }

    @Test
    public void testDestroyOnVirtualServer() {
        when (vm.spec.isVirtualMachine()).thenReturn(true);
        try {
            resource.destroyBackupStorage(vmId);
        } catch (Vps4Exception e) {
            assertEquals("INVALID_SERVER", e.getId());
        }
        verify(actionService, never()).createAction(vmId, ActionType.DESTROY_BACKUP_STORAGE,
                                                    "{}", user.getUsername());
    }

    @Test
    public void testGetBackupStorage() {
        BackupStorage backup = resource.getBackupStorage(vmId);
        verify(vmResource, times(1)).getVm(vmId);
        verify(vmService, times(1)).getBackupStorage(hfsVmId);
        assertEquals(backupStorage, backup);
    }

    @Test
    public void testGetOnVirtualServer() {
        when (vm.spec.isVirtualMachine()).thenReturn(true);
        try {
            resource.getBackupStorage(vmId);
        } catch (Vps4Exception e) {
            assertEquals("INVALID_SERVER", e.getId());
        }
        verify(vmService, never()).getBackupStorage(hfsVmId);
    }

    @Test
    public void testResetBackupStorageCreds() {
        resource.resetBackupStorageCreds(vmId);
        verify(vmResource, times(1)).getVm(vmId);
        verify(actionService, times(1)).createAction(vmId, ActionType.RESET_BACKUP_STORAGE_CREDS,
                                                     "{}", user.getUsername());
        ArgumentMatcher<CommandGroupSpec> filter = new ArgumentMatcher<CommandGroupSpec>() {
            @Override
            public boolean matches(Object item) {
                CommandGroupSpec spec = (CommandGroupSpec) item;
                return spec.commands.get(0).command.equals("Vps4ResetBackupStorageCreds");
            }
        };
        verify(commandService, times(1)).executeCommand(argThat(filter));
    }

    @Test
    public void testDoubleResetCreds() {
        Action action = mock(Action.class);
        action.type = ActionType.RESET_BACKUP_STORAGE_CREDS;
        action.status = ActionStatus.NEW;
        ResultSubset<Action> currentActions = new ResultSubset<>(Collections.singletonList(action), 1);
        when(actionService.getActionList(any())).thenReturn(currentActions);
        try {
            resource.resetBackupStorageCreds(vmId);
        } catch (Vps4Exception e) {
            assertEquals("CONFLICTING_INCOMPLETE_ACTION", e.getId());
        }
        verify(actionService, never()).createAction(vmId, ActionType.RESET_BACKUP_STORAGE_CREDS,
                                                    "{}", user.getUsername());
    }

    @Test
    public void testResetOnVirtualServer() {
        when (vm.spec.isVirtualMachine()).thenReturn(true);
        try {
            resource.resetBackupStorageCreds(vmId);
        } catch (Vps4Exception e) {
            assertEquals("INVALID_SERVER", e.getId());
        }
        verify(actionService, never()).createAction(vmId, ActionType.RESET_BACKUP_STORAGE_CREDS,
                                                    "{}", user.getUsername());
    }

    @Test
    public void testGetBackupStorageCreds() {
        BackupStorageCreds creds = resource.getBackupStorageCreds(vmId);
        verify(vmResource, times(1)).getVm(vmId);
        verify(vmService, times(1)).getBackupStorageCreds(hfsVmId);
        assertEquals(backupStorageCreds, creds);
        verify(backupStorageService, never()).getBackupStorage(vmId);
    }

    @Test
    public void testGetBackupStorageCredsFromDatabase() {
        when(vmService.getBackupStorageCreds(hfsVmId)).thenThrow(new NotFoundException(""));
        BackupStorageCreds creds = resource.getBackupStorageCreds(vmId);
        verify(vmResource, times(1)).getVm(vmId);
        verify(vmService, times(1)).getBackupStorageCreds(hfsVmId);
        assertEquals("example.com", creds.ftpServer);
        assertEquals("ftp", creds.ftpUser);
        verify(backupStorageService, times(1)).getBackupStorage(vmId);
    }

    @Test
    public void testGetCredsOnVirtualServer() {
        when (vm.spec.isVirtualMachine()).thenReturn(true);
        try {
            resource.getBackupStorageCreds(vmId);
        } catch (Vps4Exception e) {
            assertEquals("INVALID_SERVER", e.getId());
        }
        verify(vmService, never()).getBackupStorageCreds(hfsVmId);
    }
}
