package com.godaddy.vps4.web.console;

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.UUID;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.core.HttpHeaders;

import org.junit.Before;
import org.junit.Test;

import com.godaddy.vps4.console.ConsoleService;
import com.godaddy.vps4.console.CouldNotRetrieveConsoleException;
import com.godaddy.vps4.vm.ServerSpec;
import com.godaddy.vps4.vm.ServerType;
import com.godaddy.vps4.vm.VirtualMachine;
import com.godaddy.vps4.web.vm.VmResource;

public class ConsoleResourceTest {

    ConsoleService consoleService = mock(ConsoleService.class);
    VmResource vmResource = mock(VmResource.class);

    String fakeSpiceUrl =
            "https://console.phx-public.cloud.secureserver.net:443/spice_auto" +
                    ".html?token=394f9629-4081-421d-a2e3-30b7aa950843";
    String fakeOvhUrl = "https://ovh1.some.host:443/kvm/console/url";
    String fromIpAddress = "2.2.2.2";
    UUID vmId = UUID.randomUUID();
    long hfsVmId = 1234;
    VirtualMachine vm;
    ConsoleResource.Console fakeConsole = new ConsoleResource.Console(fakeSpiceUrl);
    ConsoleResource consoleResource = new ConsoleResource(consoleService, vmResource);

    @Before
    public void setupTest() {
        vm = new VirtualMachine();
        vm.hfsVmId = hfsVmId;
        vm.spec = new ServerSpec();
        vm.spec.serverType = new ServerType();
        vm.spec.serverType.serverType = ServerType.Type.VIRTUAL;

        when(consoleService.getConsoleUrl(hfsVmId)).thenReturn(fakeSpiceUrl);
        when(consoleService.getConsoleUrl(eq(hfsVmId), anyString())).thenReturn(fakeOvhUrl);
        when(vmResource.getVm(vmId)).thenReturn(vm);
    }

    @Test
    public void forAVMReturnsConsoleWithHfsUrl() {
        ConsoleResource.Console actualConsole = consoleResource.getConsoleUrl(vmId, null, null, null);
        verify(consoleService, times(1)).getConsoleUrl(hfsVmId);
        assertEquals(fakeSpiceUrl, actualConsole.url);
    }

    @Test
    public void forADEDWithFromIpAddressPassedInReturnsConsoleWithHfsUrl() {
        String ipAddress = "12.12.12.12";
        vm.spec.serverType.serverType = ServerType.Type.DEDICATED;

        ConsoleResource.Console actualConsole = consoleResource.getConsoleUrl(vmId, ipAddress, null, null);

        verify(consoleService, times(1)).getConsoleUrl(hfsVmId, ipAddress);
        assertEquals(fakeOvhUrl, actualConsole.url);
    }

    @Test
    public void forADEDWithFromIpAddressNotPassedInReturnsConsoleWithHfsUrl() {
        HttpHeaders httpHeaders = mock(HttpHeaders.class);
        HttpServletRequest httpServletRequest = mock(HttpServletRequest.class);
        when(httpServletRequest.getRemoteAddr()).thenReturn(fromIpAddress);
        vm.spec.serverType.serverType = ServerType.Type.DEDICATED;

        ConsoleResource.Console actualConsole =
                consoleResource.getConsoleUrl(vmId, null, httpHeaders, httpServletRequest);

        verify(consoleService, times(1)).getConsoleUrl(hfsVmId, fromIpAddress);
        assertEquals(fakeOvhUrl, actualConsole.url);
    }

    @Test
    public void forADEDWithInvalidFromIpAddressReturnsBlankUrl() {
        String ipAddress = "notanip";
        vm.spec.serverType.serverType = ServerType.Type.DEDICATED;

        ConsoleResource.Console actualConsole = consoleResource.getConsoleUrl(vmId, ipAddress, null, null);

        verify(consoleService, never()).getConsoleUrl(hfsVmId, ipAddress);
        assertEquals("", actualConsole.url);
    }

    @Test
    public void handlesCouldNotRetrieveConsoleException() {
        when(consoleService.getConsoleUrl(hfsVmId)).thenThrow(new CouldNotRetrieveConsoleException("..."));
        ConsoleResource.Console actualConsole = consoleResource.getConsoleUrl(vmId, null, null, null);
        assertEquals("", actualConsole.url);
    }

    @Test
    public void handlesRandomException() {
        when(consoleService.getConsoleUrl(hfsVmId)).thenThrow(new RuntimeException());
        ConsoleResource.Console actualConsole = consoleResource.getConsoleUrl(vmId, null, null, null);
        assertEquals("", actualConsole.url);
    }
}