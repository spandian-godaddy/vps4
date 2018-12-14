package com.godaddy.vps4.console;

import static org.junit.Assert.assertEquals;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.godaddy.hfs.config.Config;
import com.godaddy.hfs.vm.VmAction;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import com.godaddy.hfs.vm.Console;
import com.godaddy.hfs.vm.VmService;

public class SpiceConsoleTest {

    VmService vmService = mock(VmService.class);
    Config config = mock(Config.class);

    long hfsVmId = 12345;
    String fakeUrl = "https://console.phx-public.cloud.secureserver.net:443/spice_auto.html?token=394f9629-4081-421d-a2e3-30b7aa950843";
    Console hfsConsole;
    VmAction completeHfsVmAction;
    VmAction inProgressHfsVmAction;
    ConsoleService consoleService;

    @Before
    public void setUp(){
        hfsConsole = new Console();
        hfsConsole.url = fakeUrl;

        completeHfsVmAction = new VmAction();
        completeHfsVmAction.vmId = hfsVmId;
        completeHfsVmAction.state = VmAction.Status.COMPLETE;

        inProgressHfsVmAction = new VmAction();
        inProgressHfsVmAction.vmId = hfsVmId;
        inProgressHfsVmAction.state = VmAction.Status.IN_PROGRESS;

        when(vmService.createConsoleUrl(hfsVmId)).thenReturn(inProgressHfsVmAction);
        when(vmService.getVmAction(hfsVmId, inProgressHfsVmAction.vmActionId)).thenReturn(completeHfsVmAction);
        when(config.get("ded4.console.deployed", "false")).thenReturn("false");
        when(vmService.getConsole(hfsVmId)).thenReturn(hfsConsole);

        consoleService = new SpiceConsole(config, vmService){
            @Override
            void sleepOneSecond() {}
        };
    }

    @Test
    public void testGetSpiceConsoleUrlPassesHfsUrlThrough() {
        String actualConsoleUrl = consoleService.getConsoleUrl(hfsVmId);
        assertEquals(fakeUrl, actualConsoleUrl);
    }

    @Test(expected = CouldNotRetrieveConsoleException.class)
    public void testIfHfsReturnsNull() {
        when(vmService.getConsole(hfsVmId)).thenReturn(null);
        consoleService.getConsoleUrl(hfsVmId);
    }

    @Test(expected = CouldNotRetrieveConsoleException.class)
    public void testIfHfsReturnsNullUrl() {
        hfsConsole.url = null;
        consoleService.getConsoleUrl(hfsVmId);
    }

    @Test(expected = CouldNotRetrieveConsoleException.class)
    public void testIfHfsReturnsEmptyUrl() {
        hfsConsole.url = "";
        consoleService.getConsoleUrl(hfsVmId);
    }

    @Test(expected = RuntimeException.class)
    public void testIfHfsCreateUrlFails() {
        when(config.get("ded4.console.deployed", "false")).thenReturn("true");
        inProgressHfsVmAction.state = VmAction.Status.ERROR;
        consoleService.getConsoleUrl(hfsVmId);
    }

    @Test
    public void testPollForCreateUrlToComplete() {
        when(config.get("ded4.console.deployed", "false")).thenReturn("true");
        consoleService.getConsoleUrl(hfsVmId);
        verify(vmService, times(1)).getVmAction(hfsVmId, inProgressHfsVmAction.vmActionId);
    }

    @Test
    public void testPollHasATimeout() {
        when(config.get("ded4.console.deployed", "false")).thenReturn("true");
        completeHfsVmAction.state = VmAction.Status.IN_PROGRESS;
        try {
            consoleService.getConsoleUrl(hfsVmId);
            Assert.fail("Should have thrown a runtime exception");
        }catch(RuntimeException e) {
            // Expected
        }
        verify(vmService, times(60)).getVmAction(hfsVmId, inProgressHfsVmAction.vmActionId);
    }
}
