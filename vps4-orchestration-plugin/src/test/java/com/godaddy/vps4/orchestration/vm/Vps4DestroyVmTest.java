package com.godaddy.vps4.orchestration.vm;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyLong;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.UUID;

import javax.ws.rs.ClientErrorException;
import javax.ws.rs.core.Response;

import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import com.godaddy.hfs.mailrelay.MailRelay;
import com.godaddy.hfs.mailrelay.MailRelayService;
import com.godaddy.hfs.mailrelay.MailRelayUpdate;
import com.godaddy.hfs.vm.VmAction;
import com.godaddy.hfs.vm.VmService;
import com.godaddy.vps4.hfs.HfsVmTrackingRecord;
import com.godaddy.vps4.hfs.HfsVmTrackingRecordService;
import com.godaddy.vps4.network.IpAddress;
import com.godaddy.vps4.network.IpAddress.IpAddressType;
import com.godaddy.vps4.network.NetworkService;
import com.godaddy.vps4.orchestration.TestCommandContext;
import com.godaddy.vps4.orchestration.hfs.network.ReleaseIp;
import com.godaddy.vps4.orchestration.hfs.network.UnbindIp;
import com.godaddy.vps4.orchestration.hfs.vm.DestroyVm;
import com.godaddy.vps4.orchestration.monitoring.Vps4RemoveMonitoring;
import com.godaddy.vps4.scheduledJob.ScheduledJobService;
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
import gdg.hfs.vhfs.plesk.PleskAction;
import gdg.hfs.vhfs.plesk.PleskService;
import junit.framework.Assert;

public class Vps4DestroyVmTest {

    ActionService actionService = mock(ActionService.class);
    NetworkService networkService = mock(NetworkService.class);
    VirtualMachineService virtualMachineService = mock(VirtualMachineService.class);
    gdg.hfs.vhfs.network.NetworkServiceV2 hfsNetworkService = mock(gdg.hfs.vhfs.network.NetworkServiceV2.class);
    CPanelService cpanelService = mock(CPanelService.class);
    PleskService pleskService = mock(PleskService.class);
    MailRelayService mailRelayService = mock(MailRelayService.class);
    VmService vmService = mock(VmService.class);
    ScheduledJobService scheduledJobService = mock(ScheduledJobService.class);
    VmUserService vmUserService = mock(VmUserService.class);
    HfsVmTrackingRecordService hfsVmTrackingRecordService = mock(HfsVmTrackingRecordService.class);
    Vps4RemoveMonitoring removeMonitoring = mock(Vps4RemoveMonitoring.class);
    DestroyVm destroyVm = mock(DestroyVm.class);

    Vps4DestroyVm command = new Vps4DestroyVm(actionService, networkService, virtualMachineService,
            cpanelService, pleskService);

    Injector injector = Guice.createInjector(binder -> {
        binder.bind(UnbindIp.class);
        binder.bind(ReleaseIp.class);
        binder.bind(gdg.hfs.vhfs.network.NetworkServiceV2.class).toInstance(hfsNetworkService);
        binder.bind(VmService.class).toInstance(vmService);
        binder.bind(VirtualMachineService.class).toInstance(virtualMachineService);
        binder.bind(CPanelService.class).toInstance(cpanelService);
        binder.bind(PleskService.class).toInstance(pleskService);
        binder.bind(MailRelayService.class).toInstance(mailRelayService);
        binder.bind(ScheduledJobService.class).toInstance(scheduledJobService);
        binder.bind(VmUserService.class).toInstance(vmUserService);
        binder.bind(HfsVmTrackingRecordService.class).toInstance(hfsVmTrackingRecordService);
        binder.bind(Vps4RemoveMonitoring.class).toInstance(removeMonitoring);
        binder.bind(DestroyVm.class).toInstance(destroyVm);
    });

    CommandContext context = spy(new TestCommandContext(new GuiceCommandProvider(injector)));

    VirtualMachine vm;
    VmActionRequest request;
    IpAddress primaryIp;
    CPanelLicense cPanelLicense;
    long nodePingAccountId = 123L;

    @Before
    public void setupTest() {
        vm = new VirtualMachine(UUID.randomUUID(), 42, UUID.randomUUID(), 1, null, "VM Name", null, null, null, null,
                null, "fake.host.name", 0, UUID.randomUUID());

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

        gdg.hfs.vhfs.network.IpAddress hfsIpAddress = new gdg.hfs.vhfs.network.IpAddress();
        hfsIpAddress.addressId = primaryIp.ipAddressId;
        hfsIpAddress.status = Status.BOUND;

        cPanelLicense = new CPanelLicense();
        cPanelLicense.vmId = vm.hfsVmId;
        cPanelLicense.licensedIp = "1.2.3.4";

        HfsVmTrackingRecord hfsVmTrackingRecord = new HfsVmTrackingRecord();
        hfsVmTrackingRecord.hfsVmId = vm.hfsVmId;
        hfsVmTrackingRecord.vmId = vm.vmId;
        hfsVmTrackingRecord.orionGuid = vm.orionGuid;
        hfsVmTrackingRecord.requested = Instant.now();

        when(virtualMachineService.getVirtualMachine(eq(request.virtualMachine.vmId))).thenReturn(vm);
        when(virtualMachineService.getVirtualMachine(eq(request.virtualMachine.hfsVmId))).thenReturn(vm);
        when(vmService.destroyVm(eq(request.virtualMachine.hfsVmId))).thenReturn(vmAction);
        when(vmService.getVmAction(Mockito.anyLong(), Mockito.anyLong())).thenReturn(vmAction);
        when(networkService.getVmIpAddresses(vm.vmId)).thenReturn(addresses);
        when(hfsNetworkService.getAddress(primaryIp.ipAddressId)).thenReturn(hfsIpAddress);
        when(hfsNetworkService.unbindIp(Mockito.anyLong(), Mockito.eq(true))).thenReturn(addressAction);
        when(hfsNetworkService.releaseIp(Mockito.anyLong())).thenReturn(addressAction);
        when(cpanelService.getLicenseFromDb(eq(request.virtualMachine.hfsVmId))).thenReturn(cPanelLicense);
        when(cpanelService.getLicenseFromDb(0)).thenReturn(new CPanelLicense());
        when(hfsVmTrackingRecordService.create(anyLong(), any(), any())).thenReturn(hfsVmTrackingRecord);

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

        verifyMailRelay();
    }

    private void verifyMailRelay() {
        ArgumentCaptor<MailRelayUpdate> argument = ArgumentCaptor.forClass(MailRelayUpdate.class);
        ArgumentCaptor<String> ipAddress = ArgumentCaptor.forClass(String.class);
        verify(mailRelayService, times(1)).setRelayQuota(ipAddress.capture(), argument.capture());
        Assert.assertEquals(0, argument.getValue().quota);
        Assert.assertEquals("1.2.3.4", ipAddress.getValue());
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
    public void destroyVmRemovesMonitoring() {
        command.execute(context, request);
        verify(context).execute(Vps4RemoveMonitoring.class, vm.vmId);
    }

}
