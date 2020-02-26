package com.godaddy.vps4.web.vm;

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyLong;
import static org.mockito.Matchers.argThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.UUID;

import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentMatcher;

import com.godaddy.vps4.credit.CreditService;
import com.godaddy.vps4.credit.VirtualMachineCredit;
import com.godaddy.vps4.jdbc.ResultSubset;
import com.godaddy.vps4.vm.Action;
import com.godaddy.vps4.vm.ActionService;
import com.godaddy.vps4.vm.ActionStatus;
import com.godaddy.vps4.vm.ActionType;
import com.godaddy.vps4.vm.VirtualMachine;
import com.godaddy.vps4.web.Vps4Exception;
import com.godaddy.vps4.web.security.GDUser;

import gdg.hfs.orchestration.CommandGroupSpec;
import gdg.hfs.orchestration.CommandService;
import gdg.hfs.orchestration.CommandState;

public class BackupStorageResourceTest {
    GDUser user = mock(GDUser.class);
    private ActionService actionService = mock(ActionService.class);
    private CommandService commandService = mock(CommandService.class);
    private CreditService creditService = mock(CreditService.class);
    private VirtualMachineCredit credit = mock(VirtualMachineCredit.class);
    private VmResource vmResource = mock(VmResource.class);

    private UUID vmId = UUID.randomUUID();
    private UUID orionGuid = UUID.randomUUID();

    private BackupStorageResource resource;

    @Before
    public void setUp() {
        VirtualMachine vm = mock(VirtualMachine.class);
        vm.orionGuid = orionGuid;
        vm.vmId = vmId;
        when(vmResource.getVm(vmId)).thenReturn(vm);

        Action action = mock(Action.class);
        when(actionService.getAction(anyLong())).thenReturn(action);

        when(credit.isDed4()).thenReturn(true);
        when(creditService.getVirtualMachineCredit(orionGuid)).thenReturn(credit);

        when(commandService.executeCommand(any())).thenReturn(new CommandState());
        resource = new BackupStorageResource(user, actionService, commandService, creditService, vmResource);
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
        ResultSubset<Action> currentActions = new ResultSubset<>(Collections.singletonList(action), 2);
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
        when(credit.isDed4()).thenReturn(false);
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
        ResultSubset<Action> currentActions = new ResultSubset<>(Collections.singletonList(action), 2);
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
        when(credit.isDed4()).thenReturn(false);
        try {
            resource.destroyBackupStorage(vmId);
        } catch (Vps4Exception e) {
            assertEquals("INVALID_SERVER", e.getId());
        }
        verify(actionService, never()).createAction(vmId, ActionType.DESTROY_BACKUP_STORAGE,
                                                    "{}", user.getUsername());
    }
}
