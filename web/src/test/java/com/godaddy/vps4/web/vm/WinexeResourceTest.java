package com.godaddy.vps4.web.vm;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;
import static org.mockito.Matchers.anyObject;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Random;
import java.util.UUID;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import com.godaddy.vps4.security.GDUserMock;
import com.godaddy.vps4.vm.Action;
import com.godaddy.vps4.vm.ActionService;
import com.godaddy.vps4.vm.ActionType;
import com.godaddy.vps4.vm.Image;
import com.godaddy.vps4.vm.VirtualMachine;
import com.godaddy.vps4.vm.VmAction;
import com.godaddy.vps4.web.Vps4Exception;
import com.godaddy.vps4.web.security.GDUser;

import gdg.hfs.orchestration.CommandGroupSpec;
import gdg.hfs.orchestration.CommandService;
import gdg.hfs.orchestration.CommandState;

@RunWith(MockitoJUnitRunner.class)
public class WinexeResourceTest {
    private GDUser user;
    private WinexeResource winexeResource;

    @Mock private VmResource vmResource;
    @Mock private ActionService actionService;
    @Mock private CommandService commandService;

    @Mock private Action action;
    @Mock private VirtualMachine vm;

    @Captor private ArgumentCaptor<CommandGroupSpec> commandCaptor;

    @Before
    public void setup() {
        user = GDUserMock.createShopper();
        vm.image = mock(Image.class);
        vm.image.operatingSystem = Image.OperatingSystem.WINDOWS;
        vm.vmId = UUID.randomUUID();
        action.id = new Random().nextLong();
        when(vmResource.getVm(vm.vmId)).thenReturn(vm);
        when(actionService.createAction(eq(vm.vmId), eq(ActionType.ENABLE_WINEXE), anyString(), anyString()))
                .thenReturn(action.id);
        when(actionService.getAction(action.id)).thenReturn(action);
        when(commandService.executeCommand(anyObject())).thenReturn(new CommandState());
        winexeResource = new WinexeResource(user, vmResource, actionService, commandService);
    }

    @Test
    public void testCallsGetVmForAuthValidation() {
        winexeResource.enableWinexe(vm.vmId);
        verify(vmResource, times(1)).getVm(vm.vmId);
    }

    @Test
    public void testCreatesActionAndExecutes() {
        VmAction vmAction = winexeResource.enableWinexe(vm.vmId);
        verify(actionService, times(1)).createAction(eq(vm.vmId), eq(ActionType.ENABLE_WINEXE), anyString(), anyString());
        verify(commandService, times(1)).executeCommand(commandCaptor.capture());
        assertEquals(action.id, vmAction.id);
        CommandGroupSpec spec = commandCaptor.getValue();
        assertEquals(1, spec.commands.size());
        assertEquals("Vps4EnableWinexe", spec.commands.get(0).command);
    }

    @Test
    public void throwsExceptionIfNotWindows() {
        vm.image.operatingSystem = Image.OperatingSystem.LINUX;
        try {
            winexeResource.enableWinexe(vm.vmId);
            fail();
        } catch (Vps4Exception e) {
            assertEquals("INVALID_IMAGE", e.getId());
        }
    }
}
