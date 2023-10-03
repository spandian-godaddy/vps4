package com.godaddy.vps4.orchestration.vm;

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.runners.MockitoJUnitRunner;

import com.godaddy.vps4.credit.CreditService;
import com.godaddy.vps4.credit.ECommCreditService;
import com.godaddy.vps4.network.IpAddress;
import com.godaddy.vps4.network.NetworkService;
import com.godaddy.vps4.orchestration.monitoring.Vps4AddMonitoring;
import com.godaddy.vps4.orchestration.panopta.ResumePanoptaMonitoring;
import com.godaddy.vps4.panopta.PanoptaDataService;
import com.godaddy.vps4.panopta.PanoptaServiceException;
import com.godaddy.vps4.panopta.jdbc.PanoptaServerDetails;
import com.godaddy.vps4.scheduler.api.web.SchedulerWebService;
import com.godaddy.vps4.vm.ActionService;
import com.godaddy.vps4.vm.DataCenter;
import com.godaddy.vps4.vm.VirtualMachine;
import com.godaddy.vps4.vm.VirtualMachineService;

import gdg.hfs.orchestration.CommandContext;

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

    @Mock private VirtualMachine vm;
    @Mock private PanoptaServerDetails panoptaServerDetails;

    private VmActionRequest request;
    private UUID backupJobId;
    private ArrayList<IpAddress> addresses;

    @Captor ArgumentCaptor<Function<CommandContext, Void>> resumeAutomaticBackupsCaptor;
    @Captor ArgumentCaptor<Function<CommandContext, Void>> clearVmCanceledCaptor;
    @Captor ArgumentCaptor<Function<CommandContext, Void>> markVmAsActiveCaptor;
    @Captor ArgumentCaptor<Function<CommandContext, Void>> markIpActivateCaptor;
    @Captor ArgumentCaptor<Function<CommandContext, Void>> updateProductMetaCaptor;
    @Captor ArgumentCaptor<Map<ECommCreditService.ProductMetaField, String>> productMetaCaptor;

    @Before
    public void setup() {
        MockitoAnnotations.initMocks(this);
        command = new Vps4MoveBack(actionService, virtualMachineService,
                                   schedulerWebService, networkService, creditService);

        request = new VmActionRequest();
        request.virtualMachine = vm;
        backupJobId = UUID.randomUUID();
        addresses = new ArrayList<>();
        addresses.add(new IpAddress());
        addresses.add(new IpAddress());
        addresses.get(0).addressId = 123456L;
        addresses.get(1).addressId = 552364L;

        vm.backupJobId = backupJobId;
        vm.vmId = UUID.randomUUID();
        vm.orionGuid = UUID.randomUUID();
        vm.hfsVmId = 42L;
        vm.primaryIpAddress = mock(IpAddress.class);
        vm.primaryIpAddress.addressId = 789012L;
        vm.dataCenter = mock(DataCenter.class);
        vm.dataCenter.dataCenterId = 2;

        when(context.getId()).thenReturn(UUID.randomUUID());
        when(virtualMachineService.getVirtualMachine(vm.vmId)).thenReturn(vm);
        when(networkService.getAllVmSecondaryAddresses(vm.hfsVmId)).thenReturn(addresses);
        when(panoptaDataService.getPanoptaServerDetails(vm.vmId)).thenReturn(panoptaServerDetails);
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
        command.execute(context, request);

        verify(context, times(1)).execute(eq("UpdateProdMeta"), updateProductMetaCaptor.capture(), eq(Void.class));
        updateProductMetaCaptor.getValue().apply(context);
        verify(creditService, times(1)).updateProductMeta(eq(vm.orionGuid), productMetaCaptor.capture());

        Map<ECommCreditService.ProductMetaField, String> capturedProdMeta = productMetaCaptor.getValue();
        assertEquals(2, capturedProdMeta.size());
        assertEquals(capturedProdMeta.get(ECommCreditService.ProductMetaField.DATA_CENTER),
                     String.valueOf(request.virtualMachine.dataCenter.dataCenterId));
        assertEquals(capturedProdMeta.get(ECommCreditService.ProductMetaField.PRODUCT_ID),
                     request.virtualMachine.vmId.toString());
    }

    @Test
    public void installsPanopta() {
        command.execute(context, request);
        verify(context, times(1)).execute(eq(Vps4AddMonitoring.class), eq(request));
    }

    @Test
    public void doesNotFailParentIfPanoptaFails() {
        when(context.execute(eq(Vps4AddMonitoring.class), any())).thenThrow(new RuntimeException("ignored-exception"));
        command.execute(context, request);
        verify(context, times(1)).execute(eq(Vps4AddMonitoring.class), eq(request));
    }
}
