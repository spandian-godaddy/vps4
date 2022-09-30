package com.godaddy.vps4.orchestration.cpanel;

import com.godaddy.vps4.cpanel.CpanelAccessDeniedException;
import com.godaddy.vps4.cpanel.CpanelTimeoutException;
import com.godaddy.vps4.cpanel.Vps4CpanelService;
import com.godaddy.vps4.credit.VirtualMachineCredit;
import com.godaddy.vps4.network.IpAddress;
import com.godaddy.vps4.vm.ActionService;
import com.godaddy.vps4.vm.VirtualMachine;
import gdg.hfs.orchestration.CommandContext;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Matchers;

import org.mockito.MockitoAnnotations;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.function.Function;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class Vps4InstallCPanelPackageTest {
    ActionService actionService = mock(ActionService.class);
    Vps4CpanelService cpanelService = mock(Vps4CpanelService.class);
    CommandContext context = mock(CommandContext.class);
    Vps4InstallCPanelPackage.Request req;
    VirtualMachine vm;
    VirtualMachineCredit credit;
    List<String> installedPackages = Collections.singletonList("testPackage");
    UUID vmId = UUID.randomUUID();
    UUID orionGuid = UUID.randomUUID();
    long hfsVmId = 42L;
    String shopperId = "test-shopper";

    @Captor
    private ArgumentCaptor<Function<CommandContext, List>> listInstalledPackagesCaptor;

    Vps4InstallCPanelPackage command;
    @Before
    public void setUp() throws CpanelTimeoutException, CpanelAccessDeniedException {
        // Needed for ActionCommands for thread local id
        when(context.getId()).thenReturn(UUID.randomUUID());

        vm = mock(VirtualMachine.class);
        vm.vmId = vmId;
        vm.orionGuid = orionGuid;
        vm.hfsVmId = hfsVmId;
        vm.primaryIpAddress = mock(IpAddress.class);

        credit = mock(VirtualMachineCredit.class);
        when(credit.getShopperId()).thenReturn(shopperId);
        when(cpanelService.listInstalledRpmPackages(vm.hfsVmId)).thenReturn(installedPackages);

        when(context.execute(eq("ListInstalledRpmPackages"),
                Matchers.<Function<CommandContext, List>>any(),
                eq(List.class))).thenReturn(installedPackages);

        req = new Vps4InstallCPanelPackage.Request();
        req.actionId = 23L;
        req.vmId = vm.vmId;
        req.hfsVmId = vm.hfsVmId;
        req.packageName = "testPackage";
        command = new Vps4InstallCPanelPackage(actionService, cpanelService);
        MockitoAnnotations.initMocks(this);

    }

    @Test
    public void executesInstallPackage() {
        command.execute(context, req);

        ArgumentCaptor<InstallPackage.Request> argument = ArgumentCaptor.forClass(InstallPackage.Request.class);
        verify(context).execute(eq(InstallPackage.class), argument.capture());
        InstallPackage.Request request = argument.getValue();
        assertEquals("testPackage", request.packageName);
        assertEquals(hfsVmId, request.hfsVmId);
    }

    @Test
    public void executesListInstalledPackages() {
        command.executeWithAction(context, req);
        verify(context, times(1)).execute(eq("ListInstalledRpmPackages"),
                listInstalledPackagesCaptor.capture(),
                eq(List.class));
        Function<CommandContext, List> lambdaValue = listInstalledPackagesCaptor.getValue();
        List<String> returnValue = lambdaValue.apply(context);
        assertSame(installedPackages, returnValue);
    }


    @Test(expected=RuntimeException.class)
    public void throwsErrorIfPackageNotInstalled() {
        when(context.execute(eq("ListInstalledRpmPackages"),
                Matchers.<Function<CommandContext, List>>any(),
                eq(List.class))).thenReturn(Collections.emptyList());
        command.executeWithAction(context, req);
        verify(context, times(1)).execute(eq("ListInstalledRpmPackages"),
                listInstalledPackagesCaptor.capture(),
                eq(List.class));
        Function<CommandContext, List> lambdaValue = listInstalledPackagesCaptor.getValue();
        lambdaValue.apply(context);
    }
}
