package com.godaddy.vps4.console;

import gdg.hfs.vhfs.vm.Console;
import gdg.hfs.vhfs.vm.VmService;
import static org.junit.Assert.assertEquals;
import org.junit.Before;
import org.junit.Test;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class SpiceConsoleTest {

    VmService vmService = mock(VmService.class);

    long hfsVmId = 12345;
    String fakeUrl = "https://console.phx-public.cloud.secureserver.net:443/spice_auto.html?token=394f9629-4081-421d-a2e3-30b7aa950843";
    Console hfsConsole;

    @Before
    public void setUp(){
        hfsConsole = new Console();
        hfsConsole.url = fakeUrl;
    }

    @Test
    public void testGetSpiceConsoleUrlPassesHfsUrlThrough() {
        when(vmService.getConsole(hfsVmId)).thenReturn(hfsConsole);
        ConsoleService consoleService = new SpiceConsole(vmService);
        String actualConsoleUrl = consoleService.getConsoleUrl(hfsVmId);
        assertEquals(fakeUrl, actualConsoleUrl);
    }

    @Test(expected = CouldNotRetrieveConsoleException.class)
    public void testIfHfsReturnsNull() {
        when(vmService.getConsole(hfsVmId)).thenReturn(null);
        ConsoleService consoleService = new SpiceConsole(vmService);
        consoleService.getConsoleUrl(hfsVmId);
    }

    @Test(expected = CouldNotRetrieveConsoleException.class)
    public void testIfHfsReturnsNullUrl() {
        hfsConsole.url = null;
        when(vmService.getConsole(hfsVmId)).thenReturn(hfsConsole);
        ConsoleService consoleService = new SpiceConsole(vmService);
        consoleService.getConsoleUrl(hfsVmId);
    }

    @Test(expected = CouldNotRetrieveConsoleException.class)
    public void testIfHfsReturnsEmptyUrl() {
        hfsConsole.url = "";
        when(vmService.getConsole(hfsVmId)).thenReturn(hfsConsole);
        ConsoleService consoleService = new SpiceConsole(vmService);
        consoleService.getConsoleUrl(hfsVmId);
    }
}