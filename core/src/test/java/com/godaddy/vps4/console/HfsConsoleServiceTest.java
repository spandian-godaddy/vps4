package com.godaddy.vps4.console;

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.MockitoAnnotations;

import com.godaddy.hfs.config.Config;
import com.godaddy.hfs.vm.Console;
import com.godaddy.hfs.vm.ConsoleRequest;
import com.godaddy.hfs.vm.VmAction;
import com.godaddy.hfs.vm.VmService;

public class HfsConsoleServiceTest {

    VmService vmService = mock(VmService.class);
    Config config = mock(Config.class);

    long hfsVmId = 12345;
    String allowedIpAddress = "1.1.1.1";
    String fakeUrl =
            "https://console.phx-public.cloud.secureserver.net:443/spice_auto" +
                    ".html?token=394f9629-4081-421d-a2e3-30b7aa950843";
    Console hfsConsole;
    VmAction completeHfsVmAction;
    VmAction inProgressHfsVmAction;
    ConsoleService consoleService;
    @Captor
    ArgumentCaptor<ConsoleRequest> consoleRequestArgumentCaptor;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        hfsConsole = new Console();
        hfsConsole.url = fakeUrl;

        completeHfsVmAction = new VmAction();
        completeHfsVmAction.vmId = hfsVmId;
        completeHfsVmAction.state = VmAction.Status.COMPLETE;

        inProgressHfsVmAction = new VmAction();
        inProgressHfsVmAction.vmId = hfsVmId;
        inProgressHfsVmAction.state = VmAction.Status.IN_PROGRESS;

        when(vmService.createConsoleUrl(eq(hfsVmId), any(ConsoleRequest.class))).thenReturn(inProgressHfsVmAction);
        when(vmService.getVmAction(hfsVmId, inProgressHfsVmAction.vmActionId)).thenReturn(completeHfsVmAction);
        when(vmService.getConsole(hfsVmId)).thenReturn(hfsConsole);

        consoleService = new HfsConsoleService(config, vmService) {
            @Override
            void sleepOneSecond() {
            }
        };
    }

    @Test
    public void getConsoleUrlForNonSpecificIp() {
        consoleService.getConsoleUrl(hfsVmId);
        verify(vmService, times(1)).getConsole(hfsVmId);
        verify(vmService, never()).createConsoleUrl(eq(hfsVmId), any(ConsoleRequest.class));
    }

    @Test
    public void getConsoleUrlReturnsUrlAsString() {
        String actualConsoleUrl = consoleService.getConsoleUrl(hfsVmId);
        assertEquals(fakeUrl, actualConsoleUrl);
    }

    @Test(expected = CouldNotRetrieveConsoleException.class)
    public void throwsExceptionIfHfsReturnsNull() {
        when(vmService.getConsole(hfsVmId)).thenReturn(null);
        consoleService.getConsoleUrl(hfsVmId);
    }

    @Test(expected = CouldNotRetrieveConsoleException.class)
    public void throwsExceptionIfHfsReturnsNullUrl() {
        hfsConsole.url = null;
        consoleService.getConsoleUrl(hfsVmId);
    }

    @Test(expected = CouldNotRetrieveConsoleException.class)
    public void throwsExceptionIfHfsReturnsEmptyUrl() {
        hfsConsole.url = "";
        consoleService.getConsoleUrl(hfsVmId);
    }

    @Test
    public void getConsoleUrlForSpecificIpPostsToHfsEndpoint() {
        consoleService.getConsoleUrl(hfsVmId, allowedIpAddress);

        verify(vmService, times(1))
                .createConsoleUrl(eq(hfsVmId), consoleRequestArgumentCaptor.capture());
        ConsoleRequest request = consoleRequestArgumentCaptor.getValue();
        assertEquals(allowedIpAddress, request.allowedAddress);
    }

    @Test
    public void getConsoleUrlForSpecificIpPollsCreateCompletion() {
        consoleService.getConsoleUrl(hfsVmId, allowedIpAddress);
        verify(vmService, times(1)).getVmAction(hfsVmId, inProgressHfsVmAction.vmActionId);
    }

    @Test
    public void pollForCreateCompletionHasATimeout() {
        completeHfsVmAction.state = VmAction.Status.IN_PROGRESS;
        try {
            consoleService.getConsoleUrl(hfsVmId, allowedIpAddress);
            Assert.fail("Should have thrown a runtime exception");
        } catch (RuntimeException e) {
            // Expected
        }
        verify(vmService, times(60)).getVmAction(hfsVmId, inProgressHfsVmAction.vmActionId);
    }

    @Test
    public void getConsoleUrlForSpecificIpReturnAfterCompletes() {
        consoleService.getConsoleUrl(hfsVmId, allowedIpAddress);
        verify(vmService, times(1)).getConsole(hfsVmId);
    }

    @Test(expected = RuntimeException.class)
    public void testIfHfsCreateUrlFails() {
        inProgressHfsVmAction.state = VmAction.Status.ERROR;
        consoleService.getConsoleUrl(hfsVmId, allowedIpAddress);
    }
}
