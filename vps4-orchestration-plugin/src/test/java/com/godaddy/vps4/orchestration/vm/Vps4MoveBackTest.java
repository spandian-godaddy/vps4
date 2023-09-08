package com.godaddy.vps4.orchestration.vm;

import com.godaddy.vps4.credit.CreditService;
import com.godaddy.vps4.credit.ECommCreditService;
import com.godaddy.vps4.network.IpAddress;
import com.godaddy.vps4.network.NetworkService;
import com.godaddy.vps4.orchestration.panopta.ResumePanoptaMonitoring;
import com.godaddy.vps4.panopta.PanoptaDataService;
import com.godaddy.vps4.scheduler.api.web.SchedulerWebService;
import com.godaddy.vps4.vm.ActionService;
import com.godaddy.vps4.vm.VirtualMachine;
import com.godaddy.vps4.vm.VirtualMachineService;
import gdg.hfs.orchestration.CommandContext;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.runners.MockitoJUnitRunner;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class Vps4MoveBackTest {
    @Mock private CommandContext context;
    @Mock private ActionService actionService;
    @Mock private VirtualMachineService virtualMachineService;
    @Mock private SchedulerWebService schedulerWebService;
    @Mock private NetworkService networkService;
    @Mock private PanoptaDataService panoptaDataService;
    @Mock private CreditService creditService;
    private Vps4MoveBack command;

    private Vps4MoveBack.Request request;
    private VirtualMachine vm;
    private UUID backupJobId;
    private ArrayList<IpAddress> addresses;

    @Captor ArgumentCaptor<Function<CommandContext, Void>> resumeAutomaticBackupsCaptor;
    @Captor ArgumentCaptor<Function<CommandContext, Void>> clearVmCanceledCaptor;
    @Captor ArgumentCaptor<Function<CommandContext, Void>> markVmAsActiveCaptor;
    @Captor ArgumentCaptor<Function<CommandContext, Void>> markPanoptaServerActiveCaptor;
    @Captor ArgumentCaptor<Function<CommandContext, Void>> markIpActivateCaptor;
    @Captor ArgumentCaptor<Function<CommandContext, Void>> updateProductMetaCaptor;
    @Captor ArgumentCaptor<Map<ECommCreditService.ProductMetaField, String>> productMetaCaptor;


    @Before
    public void setup() {
        MockitoAnnotations.initMocks(this);
        command = new Vps4MoveBack(actionService, virtualMachineService, schedulerWebService, networkService,
                panoptaDataService, creditService);

        request = new Vps4MoveBack.Request();
        request.vmId = UUID.randomUUID();
        request.orionGuid = UUID.randomUUID();
        request.dcId = 1;
        backupJobId = UUID.randomUUID();
        addresses = new ArrayList<>();
        addresses.add(new IpAddress());
        addresses.add(new IpAddress());
        addresses.get(0).addressId = 123456L;
        addresses.get(1).addressId = 552364L;

        vm = new VirtualMachine();
        vm.backupJobId = backupJobId;
        vm.vmId = request.vmId;
        vm.hfsVmId = 42L;
        vm.primaryIpAddress = new IpAddress();
        vm.primaryIpAddress.addressId = 789012L;

        when(context.getId()).thenReturn(UUID.randomUUID());
        when(virtualMachineService.getVirtualMachine(request.vmId)).thenReturn(vm);
        when(networkService.getAllVmSecondaryAddresses(vm.hfsVmId)).thenReturn(addresses);
    }

    @Test
    public void resumesAutomaticBackups() {
        command.execute(context, request);
        verify(context, times(1)).execute(eq("ResumeAutomaticBackups"),
                resumeAutomaticBackupsCaptor.capture(), eq(Void.class));
        resumeAutomaticBackupsCaptor.getValue().apply(context);
        verify(schedulerWebService, times(1)).resumeJob("vps4", "backups", backupJobId);
    }

    @Test
    public void doesNotResumeAutomaticBackupsForNullBackupJobId() {
        vm.backupJobId = null;
        command.execute(context, request);
        verify(context, never()).execute(eq("ResumeAutomaticBackups"),
                resumeAutomaticBackupsCaptor.capture(), eq(Void.class));
    }

    @Test
    public void callsToSetCanceledAndValidUntil() {
        command.execute(context, request);

        verify(context, times(1)).execute(eq("ClearVmCanceled"),
                clearVmCanceledCaptor.capture(), eq(Void.class));
        clearVmCanceledCaptor.getValue().apply(context);
        verify(virtualMachineService, times(1)).clearVmCanceled(vm.vmId);

        verify(context, times(1)).execute(eq("MarkVmAsActive"),
                markVmAsActiveCaptor.capture(), eq(Void.class));
        markVmAsActiveCaptor.getValue().apply(context);
        verify(virtualMachineService, times(1)).setVmActive(vm.vmId);
    }

    @Test
    public void marksPanoptaServerActive() {
        command.execute(context, request);

        verify(context, times(1)).execute(eq("MarkPanoptaServerActive"),
                markPanoptaServerActiveCaptor.capture(), eq(Void.class));
        markPanoptaServerActiveCaptor.getValue().apply(context);
        verify(panoptaDataService, times(1)).setPanoptaServerActive(vm.vmId);
    }

    @Test
    public void resumesPanoptaMonitoring() {
        command.execute(context, request);
        verify(context, times(1)).execute(eq(ResumePanoptaMonitoring.class), eq(vm));
    }

    @Test
    public void setValidUntilOnIpAddresses() {
        command.execute(context, request);

        verify(context, times(1)).execute(
                eq("MarkIpActive-" + addresses.get(0).addressId),
                markIpActivateCaptor.capture(),
                eq(Void.class));
        markIpActivateCaptor.getValue().apply(context);
        verify(networkService, times(1)).activateIpAddress(addresses.get(0).addressId);

        verify(context, times(1)).execute(
                eq("MarkIpActive-" + addresses.get(1).addressId),
                markIpActivateCaptor.capture(),
                eq(Void.class));
        markIpActivateCaptor.getValue().apply(context);
        verify(networkService, times(1)).activateIpAddress(addresses.get(1).addressId);
    }

    @Test
    public void updatesProdMeta() {
        Instant preExecutionInstant = Instant.now();

        command.execute(context, request);

        verify(context, times(1)).execute(eq("UpdateProdMeta"),
                updateProductMetaCaptor.capture(), eq(Void.class));
        updateProductMetaCaptor.getValue().apply(context);
        verify(creditService, times(1)).updateProductMeta(eq(request.orionGuid), productMetaCaptor.capture());

        Map<ECommCreditService.ProductMetaField, String> capturedProdMeta = productMetaCaptor.getValue();
        assertEquals(capturedProdMeta.get(ECommCreditService.ProductMetaField.DATA_CENTER), String.valueOf(request.dcId));
        assertEquals(capturedProdMeta.get(ECommCreditService.ProductMetaField.PRODUCT_ID), request.vmId.toString());
        assertTrue(preExecutionInstant.isBefore(Instant.parse(capturedProdMeta.get(ECommCreditService.ProductMetaField.PROVISION_DATE))));
    }
}
