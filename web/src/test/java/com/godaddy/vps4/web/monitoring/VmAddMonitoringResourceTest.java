package com.godaddy.vps4.web.monitoring;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;

import com.godaddy.hfs.vm.Vm;
import com.godaddy.vps4.jdbc.ResultSubset;
import com.godaddy.vps4.security.GDUserMock;
import com.godaddy.vps4.vm.Action;
import com.godaddy.vps4.vm.ActionService;
import com.godaddy.vps4.vm.ActionType;
import com.godaddy.vps4.vm.VirtualMachine;
import com.godaddy.vps4.web.Vps4Exception;
import com.godaddy.vps4.web.security.GDUser;
import com.godaddy.vps4.web.vm.VmResource;

import gdg.hfs.orchestration.CommandGroupSpec;
import gdg.hfs.orchestration.CommandService;
import gdg.hfs.orchestration.CommandState;

public class VmAddMonitoringResourceTest {

    private GDUser user = GDUserMock.createShopper();
    private VmResource vmResource = mock(VmResource.class);
    private ActionService actionService = mock(ActionService.class);
    private CommandService commandService = mock(CommandService.class);
    private VmAddMonitoringResource resource;

    private UUID vmId = UUID.randomUUID();
    private long hfsVmId = 23L;
    private Vm hfsVm;
    private ResultSubset<Action> actions;

    @Before
    public void setUp() {
        VirtualMachine vm = mock(VirtualMachine.class);
        vm.hfsVmId = hfsVmId;
        when(vmResource.getVm(vmId)).thenReturn(vm);

        hfsVm = mock(Vm.class);
        hfsVm.status = "ACTIVE";
        when(vmResource.getVmFromVmVertical(hfsVmId)).thenReturn(hfsVm);

        Action vmAction = mock(Action.class);
        when(actionService.getAction(anyLong())).thenReturn(vmAction);
        actions = new ResultSubset<>(Collections.emptyList(), 0);
        when(actionService.getActionList(any())).thenReturn(actions);

        when(commandService.executeCommand(any())).thenReturn(new CommandState());
        resource = new VmAddMonitoringResource(user, vmResource, actionService, commandService, null);
    }

    @Test
    public void createsAddMonitoringAction() {
        resource.installPanoptaAgent(vmId);
        verify(actionService).createAction(vmId, ActionType.ADD_MONITORING, "{}", user.getUsername());
    }

    @Test
    public void executesAddMonitoringCommand() {
        resource.installPanoptaAgent(vmId);

        ArgumentCaptor<CommandGroupSpec> argument = ArgumentCaptor.forClass(CommandGroupSpec.class);
        verify(commandService).executeCommand(argument.capture());
        CommandGroupSpec cmdGroup = argument.getValue();
        assertEquals("Vps4AddMonitoring", cmdGroup.commands.get(0).command);
    }

    @Test
    public void errorsIfVmNotActive() {
        hfsVm.status = "STOPPED";
        try {
            resource.installPanoptaAgent(vmId);
            fail();
        } catch (Vps4Exception e) {
            assertEquals("INVALID_STATUS", e.getId());
        }
    }

    @Test
    public void errorsIfConflictingActionPending() {
        Action conflictAction = mock(Action.class);
        List<ActionType> conflictTypes = Arrays.asList(ActionType.START_VM, ActionType.STOP_VM,
                ActionType.RESTART_VM, ActionType.ADD_MONITORING);

        for (ActionType type : conflictTypes) {
            conflictAction.type = type;
            when(actionService.getIncompleteActions(vmId)).thenReturn(Collections.singletonList(conflictAction));

            try {
                resource.installPanoptaAgent(vmId);
                fail();
            } catch (Vps4Exception e) {
                assertEquals("CONFLICTING_INCOMPLETE_ACTION", e.getId());
            }
        }

    }

}
