package com.godaddy.vps4.web.console;

import com.godaddy.vps4.console.ConsoleService;
import com.godaddy.vps4.console.CouldNotRetrieveConsoleException;
import com.godaddy.vps4.vm.VirtualMachine;
import com.godaddy.vps4.web.vm.VmResource;
import static org.junit.Assert.assertEquals;
import org.junit.Before;
import org.junit.Test;

import java.util.UUID;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class ConsoleResourceTest {

    ConsoleService consoleService = mock(ConsoleService.class);
    VmResource vmResource = mock(VmResource.class);

    String fakeSpiceUrl = "https://console.phx-public.cloud.secureserver.net:443/spice_auto.html?token=394f9629-4081-421d-a2e3-30b7aa950843";
    UUID vmId = UUID.randomUUID();
    long hfsVmId = 1234;
    ConsoleResource.Console fakeConsole = new ConsoleResource.Console(fakeSpiceUrl);
    ConsoleResource consoleResource = new ConsoleResource(consoleService, vmResource);

    @Before
    public void setupTest() {
        VirtualMachine vm = new VirtualMachine();
        vm.hfsVmId = hfsVmId;
        when(consoleService.getConsoleUrl(hfsVmId)).thenReturn(fakeSpiceUrl);
        when(vmResource.getVm(vmId)).thenReturn(vm);
    }

    @Test
    public void returnsConsoleWithHfsUrl(){
        ConsoleResource.Console actualConsole = consoleResource.getConsoleUrl(vmId);
        assertEquals(fakeConsole.url, actualConsole.url);
    }

    @Test
    public void handlesCouldNotRetrieveConsoleException(){
        when(consoleService.getConsoleUrl(hfsVmId)).thenThrow(new CouldNotRetrieveConsoleException("..."));
        ConsoleResource.Console actualConsole = consoleResource.getConsoleUrl(vmId);
        assertEquals("", actualConsole.url);
    }

    @Test
    public void handlesRandomException(){
        when(consoleService.getConsoleUrl(hfsVmId)).thenThrow(new RuntimeException());
        ConsoleResource.Console actualConsole = consoleResource.getConsoleUrl(vmId);
        assertEquals("", actualConsole.url);
    }
}