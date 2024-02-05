package com.godaddy.vps4.orchestration.vm;

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyLong;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Matchers.startsWith;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.function.Function;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import com.godaddy.hfs.config.Config;
import com.godaddy.vps4.network.NetworkService;
import com.godaddy.vps4.orchestration.hfs.sysadmin.UninstallPanoptaAgent;
import com.godaddy.vps4.orchestration.monitoring.RemovePanoptaMonitoring;
import com.godaddy.vps4.orchestration.sysadmin.Vps4RemoveSupportUser;
import com.godaddy.vps4.panopta.PanoptaDataService;
import com.godaddy.vps4.panopta.jdbc.PanoptaServerDetails;
import com.godaddy.vps4.scheduler.api.web.SchedulerWebService;
import com.godaddy.vps4.vm.ActionService;
import com.godaddy.vps4.vm.VirtualMachine;
import com.godaddy.vps4.vm.VirtualMachineService;
import com.godaddy.vps4.vm.VmUser;
import com.godaddy.vps4.vm.VmUserService;
import com.godaddy.vps4.vm.VmUserType;

import gdg.hfs.orchestration.CommandContext;

@RunWith(MockitoJUnitRunner.class)
public class Vps4MoveOutTest {
    @Mock private CommandContext context;
    @Mock private Config config;
    @Mock private ActionService actionService;
    @Mock private VirtualMachineService virtualMachineService;
    @Mock private VmUserService vmUserService;
    @Mock private SchedulerWebService schedulerWebService;
    @Mock private NetworkService networkService;
    @Mock private PanoptaDataService panoptaDataService;

    @Captor private ArgumentCaptor<Function<CommandContext, Void>> voidCommandCaptor;

    private Vps4MoveOut command;
    private Vps4MoveOut.Request request;
    private VirtualMachine vm;

    @Captor private ArgumentCaptor<Vps4RemoveSupportUser.Request> removeSupportUserRequestCaptor;
    @Captor private ArgumentCaptor<RemovePanoptaMonitoring.Request> removeMonitoringCaptor;

    @Before
    public void setUp() {
        command = new Vps4MoveOut(actionService, virtualMachineService,
                                  vmUserService, schedulerWebService, networkService);

        request = new Vps4MoveOut.Request();
        request.vmId = UUID.randomUUID();
        request.backupJobId = UUID.randomUUID();
        request.hfsVmId = 42L;
        request.addressIds = new ArrayList<>();
        request.addressIds.add(123456L);
        request.addressIds.add(552364L);

        vm = new VirtualMachine();
        vm.backupJobId = request.backupJobId;
        vm.vmId = request.vmId;
        vm.hfsVmId = 42L;
        vm.orionGuid = UUID.randomUUID();

        VmUser[] supportUserArray = new VmUser[] {
                new VmUser("lobster", request.vmId, true, VmUserType.SUPPORT),
                new VmUser("duck", request.vmId, true, VmUserType.SUPPORT)
        };
        List<VmUser> supportUsers = Arrays.asList(supportUserArray);

        when(context.getId()).thenReturn(UUID.randomUUID());
        when(virtualMachineService.getVirtualMachine(request.vmId)).thenReturn(vm);
        when(virtualMachineService.getOrionGuidByVmId(request.vmId)).thenReturn(vm.orionGuid);
        when(vmUserService.listUsers(request.vmId, VmUserType.SUPPORT)).thenReturn(supportUsers);
        when(config.get("panopta.api.templates.webhook")).thenReturn("test-template-id");
        setUpPanoptaServerDetails();
    }

    private void setUpPanoptaServerDetails() {
        PanoptaServerDetails panoptaServerDetails = new PanoptaServerDetails();
        panoptaServerDetails.setServerId(1234);
        panoptaServerDetails.setPartnerCustomerKey("test-customer");
        when(panoptaDataService.getPanoptaServerDetails(vm.vmId)).thenReturn(panoptaServerDetails);
    }

    @Test
    public void removesAllSupportUsers() {
        command.execute(context, request);
        verify(context, times(2)).execute(startsWith("AttemptRemoveUser-"), voidCommandCaptor.capture(), eq(Void.class));

        voidCommandCaptor.getAllValues().forEach(f -> f.apply(context));
        verify(context, times(2)).execute(startsWith("RemoveUser-"),
                                          eq(Vps4RemoveSupportUser.class),
                                          removeSupportUserRequestCaptor.capture());

        List<Vps4RemoveSupportUser.Request> results = removeSupportUserRequestCaptor.getAllValues();
        assertEquals("lobster", results.get(0).username);
        assertEquals("duck", results.get(1).username);
    }

    @Test
    public void ignoresSupportUserRemovalExceptions() {
        when(context.execute(anyString(), eq(Vps4RemoveSupportUser.class), any()))
                .thenThrow(new RuntimeException("test exception"));
        removesAllSupportUsers();
    }

    @Test
    public void pausesAutomaticBackups() {
        command.execute(context, request);
        verify(context, times(1)).execute(eq("PauseAutomaticBackups"), voidCommandCaptor.capture(), eq(Void.class));
        voidCommandCaptor.getValue().apply(context);
        verify(schedulerWebService, times(1)).pauseJob("vps4", "backups", vm.backupJobId);
    }

    @Test
    public void doesNotPauseAutomaticBackupsForNullBackupJobId() {
        request.backupJobId = null;
        command.execute(context, request);
        verify(schedulerWebService, never()).pauseJob("vps4", "backups", request.backupJobId);
    }

    @Test
    public void callsToSetCanceledAndValidUntil() {
        command.execute(context, request);

        verify(context, times(1)).execute(eq("MarkVmAsZombie"), voidCommandCaptor.capture(), eq(Void.class));
        voidCommandCaptor.getValue().apply(context);
        verify(virtualMachineService, times(1)).setVmCanceled(vm.vmId);

        verify(context, times(1)).execute(eq("MarkVmAsRemoved"), voidCommandCaptor.capture(), eq(Void.class));
        voidCommandCaptor.getValue().apply(context);
        verify(virtualMachineService, times(1)).setVmRemoved(vm.vmId);
    }

    @Test
    public void setValidUntilOnIpAddresses() {
        command.execute(context, request);

        verify(context, times(1)).execute(
                eq("MarkIpDeleted-" + request.addressIds.get(0).toString()),
                voidCommandCaptor.capture(),
                eq(Void.class));
        voidCommandCaptor.getValue().apply(context);
        verify(networkService, times(1)).destroyIpAddress(request.addressIds.get(0));

        verify(context, times(1)).execute(
                eq("MarkIpDeleted-" + request.addressIds.get(1).toString()),
                voidCommandCaptor.capture(),
                eq(Void.class));
        voidCommandCaptor.getValue().apply(context);
        verify(networkService, times(1)).destroyIpAddress(request.addressIds.get(1));
    }

    @Test
    public void removesPanoptaMonitoring() {
        command.execute(context, request);
        verify(context, times(1)).execute(UninstallPanoptaAgent.class, vm.hfsVmId);
        verify(context, times(1)).execute(eq(RemovePanoptaMonitoring.class), removeMonitoringCaptor.capture());

        RemovePanoptaMonitoring.Request r = removeMonitoringCaptor.getValue();
        assertEquals(vm.vmId, r.vmId);
        assertEquals(vm.orionGuid, r.orionGuid);
    }

    @Test
    public void ignoresPanoptaUninstallationException() {
        when(context.execute(eq(UninstallPanoptaAgent.class), anyLong()))
                .thenThrow(new RuntimeException("test exception"));
        command.execute(context, request);
        verify(context, times(1)).execute(UninstallPanoptaAgent.class, vm.hfsVmId);
        verify(context, times(1)).execute(eq(RemovePanoptaMonitoring.class), removeMonitoringCaptor.capture());

        RemovePanoptaMonitoring.Request r = removeMonitoringCaptor.getValue();
        assertEquals(request.vmId, r.vmId);
        assertEquals(vm.orionGuid, r.orionGuid);
    }
}
