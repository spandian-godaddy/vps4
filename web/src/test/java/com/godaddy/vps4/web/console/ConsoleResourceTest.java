package com.godaddy.vps4.web.console;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.UUID;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.ClientErrorException;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.Response;

import org.junit.Before;
import org.junit.Test;

import com.godaddy.hfs.vm.Console;
import com.godaddy.hfs.vm.VmService;
import com.godaddy.vps4.jdbc.ResultSubset;
import com.godaddy.vps4.security.GDUserMock;
import com.godaddy.vps4.vm.Action;
import com.godaddy.vps4.vm.ActionService;
import com.godaddy.vps4.vm.ActionStatus;
import com.godaddy.vps4.vm.ActionType;
import com.godaddy.vps4.vm.ServerSpec;
import com.godaddy.vps4.vm.ServerType;
import com.godaddy.vps4.vm.VirtualMachine;
import com.godaddy.vps4.web.Vps4Exception;
import com.godaddy.vps4.web.security.GDUser;
import com.godaddy.vps4.web.vm.VmResource;

import gdg.hfs.orchestration.CommandService;
import gdg.hfs.orchestration.CommandState;

public class ConsoleResourceTest {
    GDUser user = GDUserMock.createShopper();
    ActionService actionService = mock(ActionService.class);
    CommandService commandService = mock(CommandService.class);
    VmResource vmResource = mock(VmResource.class);
    com.godaddy.hfs.vm.VmService vmService = mock(VmService.class);

    String fakeUrl = "https://console.phx-public.cloud.secureserver.net:443/spice_auto" +
            ".html?token=394f9629-4081-421d-a2e3-30b7aa950843";
    String fromIpAddress = "2.2.2.2";
    UUID vmId = UUID.randomUUID();
    long hfsVmId = 1234;
    VirtualMachine vm;
    ConsoleResource consoleResource = new ConsoleResource(user, actionService, commandService, vmResource, vmService);

    @Before
    public void setupTest() {
        vm = new VirtualMachine();
        vm.vmId = vmId;
        vm.hfsVmId = hfsVmId;
        vm.spec = new ServerSpec();
        vm.spec.serverType = new ServerType();
        vm.spec.serverType.serverType = ServerType.Type.VIRTUAL;

        when(vmResource.getVm(vmId)).thenReturn(vm);
        Action action = mock(Action.class);
        when(actionService.getAction(anyLong())).thenReturn(action);
        when(commandService.executeCommand(any())).thenReturn(new CommandState());
        Console console = new Console();
        console.url = fakeUrl;
        when(vmService.getConsole(hfsVmId)).thenReturn(console);
    }

    @Test
    public void virtualWithIpParamRequestsConsoleUrl() {
        String ipAddress = "12.12.12.12";
        try {
            consoleResource.requestConsoleUrl(vmId, ipAddress, null, null);
            fail();
        } catch (Vps4Exception e) {
            assertEquals("INVALID_SERVER", e.getId());
        }
    }

    @Test
    public void dedicatedWithIpParamRequestsConsoleUrl() {
        String ipAddress = "12.12.12.12";
        vm.spec.serverType.serverType = ServerType.Type.DEDICATED;
        consoleResource.requestConsoleUrl(vmId, ipAddress, null, null);
        verify(actionService, times(1))
                .createAction(vmId, ActionType.REQUEST_CONSOLE, "{}", user.getUsername());
    }

    @Test
    public void dedicatedWithoutIpParamRequestsConsoleUrl() {
        HttpHeaders httpHeaders = mock(HttpHeaders.class);
        HttpServletRequest httpServletRequest = mock(HttpServletRequest.class);
        when(httpServletRequest.getRemoteAddr()).thenReturn(fromIpAddress);
        vm.spec.serverType.serverType = ServerType.Type.DEDICATED;

        consoleResource.requestConsoleUrl(vmId, null, httpHeaders, httpServletRequest);
        verify(actionService, times(1))
                .createAction(vmId, ActionType.REQUEST_CONSOLE, "{}", user.getUsername());
    }

    @Test
    public void dedicatedWithInvalidIpParamReturnsError() {
        String ipAddress = "notanip";
        vm.spec.serverType.serverType = ServerType.Type.DEDICATED;
        try {
            consoleResource.requestConsoleUrl(vmId, ipAddress, null, null);
            fail();
        } catch (Vps4Exception e) {
            assertEquals("INVALID_CLIENT_IP", e.getId());
        }
    }

    @Test
    public void testDoubleDedicatedRequestsConsoleUrl() {
        Action action = mock(Action.class);
        action.type = ActionType.REQUEST_CONSOLE;
        action.status = ActionStatus.NEW;
        ResultSubset<Action> currentActions = new ResultSubset<>(Collections.singletonList(action), 1);
        when(actionService.getActionList(any())).thenReturn(currentActions);
        try {
            String ipAddress = "12.12.12.12";
            vm.spec.serverType.serverType = ServerType.Type.DEDICATED;
            consoleResource.requestConsoleUrl(vmId, ipAddress, null, null);
            fail();
        } catch (Vps4Exception e) {
            assertEquals("CONFLICTING_INCOMPLETE_ACTION", e.getId());
        }
        verify(actionService, never()).createAction(vmId, ActionType.REQUEST_CONSOLE, "{}", user.getUsername());
    }

    @Test
    public void vmReturnsConsoleUrl() {
        ConsoleResource.Console actualConsole = consoleResource.getConsoleUrl(vmId);
        verify(vmService, times(1)).getConsole(hfsVmId);
        assertEquals(fakeUrl, actualConsole.url);
    }

    @Test
    public void dedicatedReturnsConsoleUrl() {
        vm.spec.serverType.serverType = ServerType.Type.DEDICATED;
        ConsoleResource.Console actualConsole = consoleResource.getConsoleUrl(vmId);
        verify(vmService, times(1)).getConsole(hfsVmId);
        assertEquals(fakeUrl, actualConsole.url);
    }

    @Test
    public void handlesEmptyUrl() {
        when(vmService.getConsole(hfsVmId)).thenReturn(new Console());
        try {
            consoleResource.getConsoleUrl(vmId);
            fail();
        } catch (Vps4Exception e) {
            assertEquals("EMPTY_CONSOLE_URL", e.getId());
        }
    }

    @Test
    public void handlesInternalServerException() {
        // HFS throws a ClientErrorException when you call GET /console without first calling POST /console
        when(vmService.getConsole(hfsVmId)).thenThrow(new ClientErrorException(Response.Status.CONFLICT));
        try {
            consoleResource.getConsoleUrl(vmId);
            fail();
        } catch (Vps4Exception e) {
            assertEquals("CONSOLE_URL_FAILED", e.getId());
        }
    }
}