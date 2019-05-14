package com.godaddy.vps4.web.vm;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.fail;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import org.junit.Before;
import org.junit.Test;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.godaddy.hfs.vm.Vm;
import com.godaddy.hfs.vm.VmService;
import com.godaddy.vps4.orchestration.vm.Vps4Rescue;
import com.godaddy.vps4.security.GDUserMock;
import com.godaddy.vps4.vm.Action;
import com.godaddy.vps4.vm.ActionService;
import com.godaddy.vps4.vm.ActionType;
import com.godaddy.vps4.vm.VirtualMachine;
import com.godaddy.vps4.web.Vps4Exception;
import com.godaddy.vps4.web.action.ActionResource;
import com.godaddy.vps4.web.security.GDUser;

import gdg.hfs.orchestration.CommandService;
import gdg.hfs.orchestration.CommandState;

public class VmRescueResourceTest {

    private GDUser gdUser = GDUserMock.createShopper();
    private VmResource vmResource = mock(VmResource.class);
    private ActionService actionService = mock(ActionService.class);
    private CommandService commandService = mock(CommandService.class);
    private VmRescueResource rescueResource;
    private ActionResource actionResource = mock(ActionResource.class);
    private VmService vmService = mock(VmService.class);

    private UUID vmId = UUID.randomUUID();
    private long hfsVmId = 23L;
    private Vm hfsVm;

    @Before
    public void testSetup() {
        VirtualMachine vm = mock(VirtualMachine.class);
        vm.hfsVmId = hfsVmId;
        when(vmResource.getVm(vmId)).thenReturn(vm);

        hfsVm = mock(Vm.class);
        when(vmResource.getVmFromVmVertical(hfsVmId)).thenReturn(hfsVm);

        Action rescueAction = mock(Action.class);
        when(actionService.getAction(anyLong())).thenReturn(rescueAction);

        when(commandService.executeCommand(any())).thenReturn(new CommandState());
        rescueResource = new VmRescueResource(gdUser, vmResource, actionService, commandService, vmService, 
                actionResource);
    }

    @Test
    public void createsRescueAction() {
        hfsVm.status = "ACTIVE";
        rescueResource.rescue(vmId);
        verify(actionService).createAction(vmId, ActionType.RESCUE, "{}", gdUser.getUsername());
    }

    @Test
    public void throwsVps4ExceptionIfServerNotInActiveStatus() {
        hfsVm.status = "RESCUED";
        try {
            rescueResource.rescue(vmId);
            fail();
        } catch (Vps4Exception ex) {
            assertEquals("INVALID_STATUS", ex.getId());
        }
        verify(actionService, never()).createAction(vmId, ActionType.RESCUE, "{}", gdUser.getUsername());
    }

    @Test
    public void createsEndRescueAction() {
        hfsVm.status = "RESCUED";
        rescueResource.endRescue(vmId);
        verify(actionService).createAction(vmId, ActionType.END_RESCUE, "{}", gdUser.getUsername());
    }

    @Test
    public void throwsVps4ExceptionIfServerNotInRescueStatus() {
        hfsVm.status = "ACTIVE";
        try {
            rescueResource.endRescue(vmId);
            fail();
        } catch (Vps4Exception ex) {
            assertEquals("INVALID_STATUS", ex.getId());
        }
        verify(actionService, never()).createAction(vmId, ActionType.END_RESCUE, "{}", gdUser.getUsername());
    }

    @Test
    public void testGetRescueCredentials() throws JsonProcessingException {
        Action testVps4Action = mock(Action.class);
        ObjectMapper mapper = new ObjectMapper();
        Vps4Rescue.Response response = new Vps4Rescue.Response();
        response.hfsVmActionId = 123L;
        testVps4Action.response = mapper.writeValueAsString(response);
        List<Action> testVps4Actions = Arrays.asList(testVps4Action);
        when(actionResource.getVmActionList(vmId, Arrays.asList("COMPLETE"), Arrays.asList("RESCUE"), null, null, 1,
                0)).thenReturn(testVps4Actions);
        com.godaddy.hfs.vm.VmAction testHfsAction = new com.godaddy.hfs.vm.VmAction();
        testHfsAction.vmActionId = 123L;
        testHfsAction.resultset = "{\"password\": \"S5wurpP1Rd6xVbYLi\", \"username\": \"root\"}";
        when(vmService.getVmAction(hfsVmId, response.hfsVmActionId)).thenReturn(testHfsAction);

        RescueCredentials creds = rescueResource.getRescueCredentials(vmId);
        assertEquals("root", creds.getUsername());
        assertEquals("S5wurpP1Rd6xVbYLi", creds.getPassword());
    }
    
    @Test
    public void testGetRescueCredentialsNoRescueAction() throws JsonProcessingException {
        List<Action> testResults = new ArrayList<Action>();
        when(actionResource.getVmActionList(vmId, Arrays.asList("COMPLETE"), Arrays.asList("RESCUE"), null, null, 1,
                0)).thenReturn(testResults);

        RescueCredentials creds = rescueResource.getRescueCredentials(vmId);

        assertNull(creds);
    }

    @Test(expected = Vps4Exception.class)
    public void testGetRescueCredentialsBadCreds() throws JsonProcessingException {
        Action testVps4Action = mock(Action.class);
        ObjectMapper mapper = new ObjectMapper();
        Vps4Rescue.Response response = new Vps4Rescue.Response();
        response.hfsVmActionId = 123L;
        testVps4Action.response = mapper.writeValueAsString(response);
        List<Action> testVps4Actions = Arrays.asList(testVps4Action);
        when(actionResource.getVmActionList(vmId, Arrays.asList("COMPLETE"), Arrays.asList("RESCUE"), null, null, 1,
                0)).thenReturn(testVps4Actions);
        com.godaddy.hfs.vm.VmAction testHfsAction = new com.godaddy.hfs.vm.VmAction();
        testHfsAction.vmActionId = 123L;
        testHfsAction.resultset = "bad creds";
        when(vmService.getVmAction(hfsVmId, response.hfsVmActionId)).thenReturn(testHfsAction);

        RescueCredentials creds = rescueResource.getRescueCredentials(vmId);
    }
}
