package com.godaddy.vps4.orchestration.vm;

import com.godaddy.hfs.config.Config;
import com.godaddy.vps4.network.NetworkService;
import com.godaddy.vps4.orchestration.panopta.PausePanoptaMonitoring;
import com.godaddy.vps4.orchestration.sysadmin.Vps4RemoveSupportUser;
import com.godaddy.vps4.panopta.PanoptaDataService;
import com.godaddy.vps4.panopta.PanoptaService;
import com.godaddy.vps4.panopta.jdbc.PanoptaServerDetails;
import com.godaddy.vps4.scheduler.api.web.SchedulerWebService;
import com.godaddy.vps4.vm.ActionService;
import com.godaddy.vps4.vm.VirtualMachine;
import com.godaddy.vps4.vm.VirtualMachineService;
import com.godaddy.vps4.vm.VmUser;
import com.godaddy.vps4.vm.VmUserService;
import com.godaddy.vps4.vm.VmUserType;
import gdg.hfs.orchestration.CommandContext;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.function.Function;

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

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
    @Mock private PanoptaService panoptaService;

    private Vps4MoveOut command;
    private Vps4MoveOut.Request request;
    private VirtualMachine vm;
    private List<VmUser> supportUsers;
    private PanoptaServerDetails panoptaServerDetails;

    @Captor private ArgumentCaptor<String> commandNameCaptor;
    @Captor private ArgumentCaptor<Vps4RemoveSupportUser.Request> removeSupportUserRequestCaptor;
    @Captor private ArgumentCaptor<Function<CommandContext, Void>> lambda;

    @Before
    public void setUp() {
        command = new Vps4MoveOut(actionService, config, virtualMachineService, vmUserService, schedulerWebService,
                                  networkService, panoptaDataService, panoptaService);

        request = new Vps4MoveOut.Request();
        request.vmId = UUID.randomUUID();
        request.backupJobId = UUID.randomUUID();
        request.hfsVmId = 42L;
        request.addressIds = new ArrayList<>();
        request.addressIds.add(123456L);
        request.addressIds.add(552364L);

        vm = new VirtualMachine();
        vm.backupJobId = UUID.randomUUID();
        vm.vmId = request.vmId;
        vm.hfsVmId = 42L;

        VmUser[] supportUserArray = new VmUser[] {
                new VmUser("lobster", request.vmId, true, VmUserType.SUPPORT),
                new VmUser("duck", request.vmId, true, VmUserType.SUPPORT)
        };
        supportUsers = Arrays.asList(supportUserArray);

        when(context.getId()).thenReturn(UUID.randomUUID());
        when(virtualMachineService.getVirtualMachine(request.vmId)).thenReturn(vm);
        when(vmUserService.listUsers(request.vmId, VmUserType.SUPPORT)).thenReturn(supportUsers);
        when(config.get("panopta.api.templates.webhook")).thenReturn("test-template-id");
        setUpPanoptaServerDetails();
    }

    private void setUpPanoptaServerDetails() {
        panoptaServerDetails = new PanoptaServerDetails();
        panoptaServerDetails.setServerId(1234);
        panoptaServerDetails.setPartnerCustomerKey("test-customer");
        when(panoptaDataService.getPanoptaServerDetails(vm.vmId)).thenReturn(panoptaServerDetails);
    }

    @Test
    public void removesAllSupportUsers() {
        List<String> capturedCommandNames;
        List<Vps4RemoveSupportUser.Request> capturedRequests;

        command.execute(context, request);

        verify(context, times(2)).execute(commandNameCaptor.capture(), eq(Vps4RemoveSupportUser.class), removeSupportUserRequestCaptor.capture());
        capturedCommandNames = commandNameCaptor.getAllValues();
        capturedRequests = removeSupportUserRequestCaptor.getAllValues();

        assertEquals("RemoveUser-lobster", capturedCommandNames.get(0));
        assertEquals("RemoveUser-duck", capturedCommandNames.get(1));
        assertEquals("lobster", capturedRequests.get(0).username);
        assertEquals("duck", capturedRequests.get(1).username);
    }

    @Test
    public void pausesAutomaticBackups() {
        command.execute(context, request);
        verify(context, times(1)).execute(eq("PauseAutomaticBackups"),
                any(Function.class), eq(Void.class));
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
        verify(context, times(1)).execute(eq("MarkVmAsZombie"),
                any(Function.class), eq(Void.class));
        verify(context, times(1)).execute(eq("MarkVmAsRemoved"),
                any(Function.class), eq(Void.class));
    }

    @Test
    public void pausesPanoptaMonitoring() {
        command.execute(context, request);
        verify(context, times(1)).execute(eq(PausePanoptaMonitoring.class), eq(request.vmId));
    }

    @Test
    public void removesPanoptaWebhook() {
        command.execute(context, request);
        verify(context, times(1)).execute(eq("RemovePanoptaWebhook"), lambda.capture(), eq(Void.class));
        lambda.getValue().apply(context);
        verify(panoptaDataService, times(1)).getPanoptaServerDetails(vm.vmId);
        verify(config, times(1)).get("panopta.api.templates.webhook");
        verify(panoptaService, times(1)).removeTemplate(1234, "test-customer", "test-template-id");
    }

    @Test
    public void setValidUntilOnIpAddresses() {
        command.execute(context, request);
        verify(context, times(1)).execute(
                eq("MarkIpDeleted-" + request.addressIds.get(0).toString()),
                any(Function.class),
                eq(Void.class));
        verify(context, times(1)).execute(
                eq("MarkIpDeleted-" + request.addressIds.get(1).toString()),
                any(Function.class),
                eq(Void.class));
    }
}
