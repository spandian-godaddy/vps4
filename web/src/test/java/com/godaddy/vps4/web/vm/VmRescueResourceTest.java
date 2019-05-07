package com.godaddy.vps4.web.vm;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.UUID;

import org.junit.Before;
import org.junit.Test;

import com.godaddy.hfs.vm.Vm;
import com.godaddy.vps4.security.GDUserMock;
import com.godaddy.vps4.vm.Action;
import com.godaddy.vps4.vm.ActionService;
import com.godaddy.vps4.vm.ActionType;
import com.godaddy.vps4.vm.VirtualMachine;
import com.godaddy.vps4.web.Vps4Exception;
import com.godaddy.vps4.web.security.GDUser;

import gdg.hfs.orchestration.CommandService;
import gdg.hfs.orchestration.CommandState;

public class VmRescueResourceTest {

    private GDUser gdUser = GDUserMock.createShopper();
    private VmResource vmResource = mock(VmResource.class);
    private ActionService actionService = mock(ActionService.class);
    private CommandService commandService = mock(CommandService.class);
    private VmRescueResource rescueResource;

    private UUID vps4VmId = UUID.randomUUID();
    private long hfsVmId = 23L;
    private Vm hfsVm;

    @Before
    public void testSetup() {
        VirtualMachine vm = mock(VirtualMachine.class);
        vm.hfsVmId = hfsVmId;
        when(vmResource.getVm(vps4VmId)).thenReturn(vm);

        hfsVm = mock(Vm.class);
        hfsVm.status = "RESCUE";
        when(vmResource.getVmFromVmVertical(hfsVmId)).thenReturn(hfsVm);

        Action rescueAction = mock(Action.class);
        when(actionService.getAction(anyLong())).thenReturn(rescueAction);

        when(commandService.executeCommand(any())).thenReturn(new CommandState());
        rescueResource = new VmRescueResource(gdUser, vmResource, actionService, commandService);
    }

    @Test
    public void createsEndRescueAction() {
        rescueResource.endRescue(vps4VmId);
        verify(actionService).createAction(vps4VmId, ActionType.END_RESCUE, "{}", gdUser.getUsername());
    }

    @Test
    public void throwsVps4ExceptionIfServerNotInRescueStatus() {
        hfsVm.status = "ACTIVE";
        try {
            rescueResource.endRescue(vps4VmId);
            fail();
        } catch (Vps4Exception ex) {
            assertEquals("INVALID_STATUS", ex.getId());
        }
        verify(actionService, never()).createAction(vps4VmId, ActionType.END_RESCUE, "{}", gdUser.getUsername());
    }

}
