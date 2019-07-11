package com.godaddy.vps4.orchestration.vm;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyLong;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.UUID;

import javax.ws.rs.ClientErrorException;
import javax.ws.rs.NotFoundException;
import javax.ws.rs.core.Response;

import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import com.godaddy.hfs.mailrelay.MailRelay;
import com.godaddy.hfs.mailrelay.MailRelayService;
import com.godaddy.hfs.mailrelay.MailRelayUpdate;
import com.godaddy.hfs.vm.Vm;
import com.godaddy.hfs.vm.VmAction;
import com.godaddy.hfs.vm.VmService;
import com.godaddy.vps4.hfs.HfsVmTrackingRecordService;
import com.godaddy.vps4.network.IpAddress;
import com.godaddy.vps4.network.IpAddress.IpAddressType;
import com.godaddy.vps4.network.NetworkService;
import com.godaddy.vps4.orchestration.TestCommandContext;
import com.godaddy.vps4.orchestration.dns.Vps4CreateDnsPtrRecord;
import com.godaddy.vps4.orchestration.hfs.dns.CreateDnsPtrRecord;
import com.godaddy.vps4.orchestration.hfs.network.ReleaseIp;
import com.godaddy.vps4.orchestration.hfs.network.UnbindIp;
import com.godaddy.vps4.orchestration.hfs.vm.DestroyVm;
import com.godaddy.vps4.scheduledJob.ScheduledJobService;
import com.godaddy.vps4.util.MonitoringMeta;
import com.godaddy.vps4.vm.ActionService;
import com.godaddy.vps4.vm.VirtualMachine;
import com.godaddy.vps4.vm.VirtualMachineService;
import com.godaddy.vps4.vm.VmUserService;
import com.google.inject.Guice;
import com.google.inject.Injector;

import gdg.hfs.orchestration.CommandContext;
import gdg.hfs.orchestration.GuiceCommandProvider;
import gdg.hfs.vhfs.cpanel.CPanelAction;
import gdg.hfs.vhfs.cpanel.CPanelLicense;
import gdg.hfs.vhfs.cpanel.CPanelService;
import gdg.hfs.vhfs.network.AddressAction;
import gdg.hfs.vhfs.network.IpAddress.Status;
import gdg.hfs.vhfs.nodeping.NodePingService;
import gdg.hfs.vhfs.plesk.PleskAction;
import gdg.hfs.vhfs.plesk.PleskService;

public class Vps4DestroyDedicatedTest {

    ActionService actionService = mock(ActionService.class);
    VirtualMachineService virtualMachineService = mock(VirtualMachineService.class);
    gdg.hfs.vhfs.network.NetworkServiceV2 hfsNetworkServiceV2 = mock(gdg.hfs.vhfs.network.NetworkServiceV2.class);
    CPanelService cpanelService = mock(CPanelService.class);
    PleskService pleskService = mock(PleskService.class);
    MailRelayService mailRelayService = mock(MailRelayService.class);
    VmService vmService = mock(VmService.class);
    NodePingService nodePingService = mock(NodePingService.class);
    ScheduledJobService scheduledJobService = mock(ScheduledJobService.class);
    VmUserService vmUserService = mock(VmUserService.class);
    MonitoringMeta monitoringMeta = mock(MonitoringMeta.class);
    NetworkService networkService = mock(NetworkService.class);
    HfsVmTrackingRecordService hfsVmTrackingRecordService = mock(HfsVmTrackingRecordService.class);
    DestroyVm destroyVm = mock(DestroyVm.class);
    Vm hfsVm = mock(Vm.class);
    CreateDnsPtrRecord createDnsPtrRecord =  mock(CreateDnsPtrRecord.class);
    Vps4CreateDnsPtrRecord vps4CreateDnsPtrRecord = mock(Vps4CreateDnsPtrRecord.class);

    Vps4DestroyDedicated command = new Vps4DestroyDedicated(actionService, networkService, nodePingService,
            monitoringMeta, hfsVmTrackingRecordService, vmService);
    Injector injector = Guice.createInjector(binder -> {
        binder.bind(UnbindIp.class);
        binder.bind(ReleaseIp.class);
        binder.bind(gdg.hfs.vhfs.network.NetworkServiceV2.class).toInstance(hfsNetworkServiceV2);
        binder.bind(VmService.class).toInstance(vmService);
        binder.bind(VirtualMachineService.class).toInstance(virtualMachineService);
        binder.bind(CPanelService.class).toInstance(cpanelService);
        binder.bind(PleskService.class).toInstance(pleskService);
        binder.bind(MailRelayService.class).toInstance(mailRelayService);
        binder.bind(NodePingService.class).toInstance(nodePingService);
        binder.bind(ScheduledJobService.class).toInstance(scheduledJobService);
        binder.bind(VmUserService.class).toInstance(vmUserService);
        binder.bind(MonitoringMeta.class).toInstance(monitoringMeta);
        binder.bind(NetworkService.class).toInstance(networkService);
        binder.bind(HfsVmTrackingRecordService.class).toInstance(hfsVmTrackingRecordService);
        binder.bind(DestroyVm.class).toInstance(destroyVm);
        binder.bind(Vm.class).toInstance(hfsVm);
        binder.bind(CreateDnsPtrRecord.class).toInstance(createDnsPtrRecord);
        binder.bind(Vps4CreateDnsPtrRecord.class).toInstance(vps4CreateDnsPtrRecord);
    });

    CommandContext context = spy(new TestCommandContext(new GuiceCommandProvider(injector)));

    VirtualMachine vm;
    VmActionRequest request;
    IpAddress primaryIp;
    CPanelLicense cPanelLicense;
    long nodePingAccountId = 123L;

    @Before
    public void setupTest() {
        vm = new VirtualMachine();
        vm.vmId = UUID.randomUUID();
        vm.hfsVmId = 42;
        vm.orionGuid = UUID.randomUUID();
        vm.projectId = 1;
        vm.name = "VM Name";
        vm.hostname = "fake.host.name";
        vm.managedLevel = 0;

        request = new VmActionRequest();
        request.virtualMachine = vm;
        request.actionId = 12;

        VmAction vmAction = new VmAction();
        vmAction.state = VmAction.Status.COMPLETE;
        vmAction.actionType = "DESTROY";

        AddressAction addressAction = new AddressAction();
        addressAction.status = AddressAction.Status.COMPLETE;

        long dummyCheckId = 5522L;
        primaryIp = new IpAddress(123, UUID.randomUUID(), "1.2.3.4", IpAddressType.PRIMARY, dummyCheckId,
                Instant.now(), Instant.now().plus(24, ChronoUnit.HOURS));
        ArrayList<IpAddress> addresses = new ArrayList<IpAddress>();
        addresses.add(primaryIp);
        vm.primaryIpAddress = primaryIp;

        gdg.hfs.vhfs.network.IpAddress hfsIpAddress = new gdg.hfs.vhfs.network.IpAddress();
        hfsIpAddress.addressId = primaryIp.ipAddressId;
        hfsIpAddress.status = Status.BOUND;

        cPanelLicense = new CPanelLicense();
        cPanelLicense.vmId = vm.hfsVmId;
        cPanelLicense.licensedIp = primaryIp.ipAddress;

        when(virtualMachineService.getVirtualMachine(eq(request.virtualMachine.vmId))).thenReturn(vm);
        when(virtualMachineService.getVirtualMachine(eq(request.virtualMachine.hfsVmId))).thenReturn(vm);
        when(vmService.destroyVm(eq(request.virtualMachine.hfsVmId))).thenReturn(vmAction);
        when(vmService.getVmAction(Mockito.anyLong(), Mockito.anyLong())).thenReturn(vmAction);
        when(vmService.getVm(eq(request.virtualMachine.hfsVmId))).thenReturn(hfsVm);

        doNothing().when(nodePingService).deleteCheck(nodePingAccountId, primaryIp.pingCheckId);
        when(monitoringMeta.getAccountId()).thenReturn(nodePingAccountId);
        when(networkService.getVmPrimaryAddress(any(UUID.class))).thenReturn(primaryIp);
        when(cpanelService.getLicenseFromDb(eq(request.virtualMachine.hfsVmId))).thenReturn(cPanelLicense);
        when(cpanelService.getLicenseFromDb(0)).thenReturn(new CPanelLicense());

        MailRelay mailRelay = new MailRelay();
        mailRelay.quota = 0;
        when(mailRelayService.setRelayQuota(eq("1.2.3.4"), any(MailRelayUpdate.class))).thenReturn(mailRelay);
    }

    @Test
    public void destroyPleskVmSuccessTest() throws Exception {

        when(virtualMachineService.virtualMachineHasPlesk(vm.vmId)).thenReturn(true);
        PleskAction action = new PleskAction();
        action.status = PleskAction.Status.COMPLETE;
        when(pleskService.licenseRelease(vm.hfsVmId)).thenReturn(action);
        MailRelay mailRelay = new MailRelay();
        mailRelay.quota = 0;
        when(mailRelayService.setRelayQuota(eq("1.2.3.4"), any(MailRelayUpdate.class))).thenReturn(mailRelay);
        command.execute(context, this.request);
        verify(pleskService, times(1)).licenseRelease(this.request.virtualMachine.hfsVmId);
        verify(nodePingService, times(1)).deleteCheck(nodePingAccountId, primaryIp.pingCheckId);
    }

    @Test
    public void destroyVmCpanelSuccessTest() throws Exception {
        when(virtualMachineService.virtualMachineHasCpanel(vm.vmId)).thenReturn(true);
        CPanelAction action = new CPanelAction();
        action.status = CPanelAction.Status.COMPLETE;
        when(cpanelService.licenseRelease(null, vm.hfsVmId)).thenReturn(action);

        command.execute(context, request);
        verify(cpanelService, times(1)).licenseRelease(null, request.virtualMachine.hfsVmId);
    }

    @Test
    public void destroyVmCpanelNotLicensedTest() throws Exception {
        cPanelLicense.licensedIp = null;
        when(virtualMachineService.virtualMachineHasCpanel(vm.vmId)).thenReturn(true);
        CPanelAction action = new CPanelAction();
        action.status = CPanelAction.Status.COMPLETE;
        when(cpanelService.licenseRelease(null, vm.hfsVmId)).thenReturn(action);

        command.execute(context, request);
        verify(cpanelService, times(0)).licenseRelease(null, request.virtualMachine.hfsVmId);
    }

    @Test
    public void destroyVmCpanelCheckLicenseErrorTest() throws Exception {
        // The actual error comes back as a 422, which client error exception can't
        // map.  vmHasCpanelLicense will catch all ClientErrorExceptions
        when(cpanelService.getLicenseFromDb(eq(request.virtualMachine.hfsVmId))).
                thenThrow(new ClientErrorException(Response.Status.EXPECTATION_FAILED));
        when(virtualMachineService.virtualMachineHasCpanel(vm.vmId)).thenReturn(true);
        CPanelAction action = new CPanelAction();
        action.status = CPanelAction.Status.COMPLETE;

        command.execute(context, request);
        verify(cpanelService, times(0)).licenseRelease(null, request.virtualMachine.hfsVmId);

    }

    @Test
    public void destroyVmPleskNotLicensedTest() throws Exception {
        when(virtualMachineService.virtualMachineHasPlesk(vm.vmId)).thenReturn(true);
        PleskAction action = new PleskAction();
        action.status = PleskAction.Status.FAILED;
        action.message = "Failed to find license for VM";
        when(pleskService.licenseRelease(vm.hfsVmId)).thenReturn(action);

        command.execute(context, request);
        verify(pleskService, times(1)).licenseRelease(request.virtualMachine.hfsVmId);
    }

    @Test(expected = RuntimeException.class)
    public void destroyVmPleskFailedTest() throws Exception {
        when(virtualMachineService.virtualMachineHasPlesk(vm.vmId)).thenReturn(true);
        PleskAction action = new PleskAction();
        action.status = PleskAction.Status.FAILED;
        action.message = "Other Error";
        when(pleskService.licenseRelease(vm.hfsVmId)).thenReturn(action);

        command.execute(context, request);
        verify(pleskService, times(0)).licenseRelease(request.virtualMachine.hfsVmId);
    }

    @Test
    public void testDeleteIpMonitoringIgnoresNotFoundException() throws Exception {
        doThrow(new NotFoundException()).when(nodePingService).deleteCheck(nodePingAccountId, primaryIp.pingCheckId);
        command.execute(context, request);
        verify(nodePingService, times(1)).deleteCheck(nodePingAccountId, primaryIp.pingCheckId);
    }

    @Test
    public void testDeleteIpMonitoringDoesntCallIfNoIp() throws Exception {
        doThrow(new NotFoundException()).when(nodePingService).deleteCheck(nodePingAccountId, primaryIp.pingCheckId);
        request.virtualMachine.primaryIpAddress = null;
        command.execute(context, request);
        verify(nodePingService, times(0)).deleteCheck(anyLong(), anyLong());
    }

    @Test
    public void ensureIpAddressRecordUpdatedForDedDestroy() throws Exception {
        command.execute(context, request);
        verify(networkService, atLeastOnce()).destroyIpAddress(anyLong());
    }

    @Test
    public void testResetPTRRecord() throws Exception {
        hfsVm.resource_id = "fake.resource_id";
        command.execute(context, request);
        verify(vmService, times(1)).getVm(anyLong());
        verify(context, times(1)).execute(any(), any(CreateDnsPtrRecord.Request.class));
    }

    @Test
    public void testResetPTRRecordFoundNoResourceId() throws Exception {
        hfsVm.resource_id = null;
        command.execute(context, request);
        verify(vmService, times(1)).getVm(anyLong());
        verify(context, never()).execute(eq(CreateDnsPtrRecord.class), any(CreateDnsPtrRecord.Request.class));
    }
}