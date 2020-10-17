package com.godaddy.vps4.orchestration.vm;

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyBoolean;
import static org.mockito.Matchers.anyLong;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Random;
import java.util.UUID;
import java.util.function.Function;

import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.MockitoAnnotations;

import com.godaddy.hfs.config.Config;
import com.godaddy.hfs.vm.Vm;
import com.godaddy.hfs.vm.VmAction;
import com.godaddy.hfs.vm.VmAction.Status;
import com.godaddy.hfs.vm.VmAddress;
import com.godaddy.hfs.vm.VmService;
import com.godaddy.vps4.credit.CreditService;
import com.godaddy.vps4.hfs.HfsVmTrackingRecordService;
import com.godaddy.vps4.messaging.Vps4MessagingService;
import com.godaddy.vps4.network.NetworkService;
import com.godaddy.vps4.orchestration.hfs.dns.CreateDnsPtrRecord;
import com.godaddy.vps4.orchestration.hfs.sysadmin.SetHostname;
import com.godaddy.vps4.orchestration.hfs.sysadmin.SetPassword;
import com.godaddy.vps4.orchestration.hfs.vm.CreateVm;
import com.godaddy.vps4.orchestration.panopta.SetupPanopta;
import com.godaddy.vps4.orchestration.vm.provision.ProvisionRequest;
import com.godaddy.vps4.orchestration.vm.provision.Vps4ProvisionDedicated;
import com.godaddy.vps4.vm.ActionService;
import com.godaddy.vps4.vm.Image;
import com.godaddy.vps4.vm.Image.ControlPanel;
import com.godaddy.vps4.vm.ProvisionVmInfo;
import com.godaddy.vps4.vm.VirtualMachine;
import com.godaddy.vps4.vm.VirtualMachineService;
import com.godaddy.vps4.vm.VmAlertService;
import com.godaddy.vps4.vm.VmMetric;
import com.godaddy.vps4.vm.VmUserService;

import gdg.hfs.orchestration.CommandContext;

public class Vps4ProvisionDedicatedTest {


    ActionService actionService = mock(ActionService.class);
    NetworkService networkService = mock(NetworkService.class);
    VirtualMachineService virtualMachineService = mock(VirtualMachineService.class);
    VmService vmService = mock(VmService.class);
    VmUserService vmUserService = mock(VmUserService.class);
    CreateVm createVm = mock(CreateVm.class);
    Vps4MessagingService messagingService = mock(Vps4MessagingService.class);
    CreditService creditService = mock(CreditService.class);
    HfsVmTrackingRecordService hfsVmTrackingRecordService = mock(HfsVmTrackingRecordService.class);
    VmAlertService vmAlertService = mock(VmAlertService.class);
    Config config = mock(Config.class);

    @Captor private ArgumentCaptor<Function<CommandContext, Void>> setCommonNameLambdaCaptor;
    @Captor private ArgumentCaptor<SetPassword.Request> setPasswordCaptor;
    @Captor private ArgumentCaptor<SetHostname.Request> setHostnameArgumentCaptor;
    @Captor private ArgumentCaptor<CreateDnsPtrRecord.Request> reverseDnsNameRequestCaptor;
    @Captor private ArgumentCaptor<SetupPanopta.Request> setupPanoptaRequestArgCaptor;

    Vps4ProvisionDedicated command = new Vps4ProvisionDedicated(actionService, vmService, virtualMachineService,
                                                                vmUserService, networkService, messagingService,
                                                                creditService, config, hfsVmTrackingRecordService,
                                                                vmAlertService);

    CommandContext context = mock(CommandContext.class);

    VirtualMachine vm;
    Vm hfsVm;
    ProvisionRequest request;
    VmAddress hfsIp;
    String expectedServerName;
    String username = "tester";
    UUID vmId = UUID.randomUUID();
    Image image;
    ProvisionVmInfo vmInfo;
    String shopperId;
    int diskGib;
    UUID orionGuid = UUID.randomUUID();
    long hfsVmId = 42;

    @Before
    public void setupTest() {
        MockitoAnnotations.initMocks(this);
        this.image = new Image();
        image.operatingSystem = Image.OperatingSystem.LINUX;
        image.controlPanel = ControlPanel.CPANEL;
        image.hfsName = "foobar";
        expectedServerName = "VM Name";
        this.vm = new VirtualMachine(UUID.randomUUID(), hfsVmId, UUID.randomUUID(), 1,
                null, expectedServerName,
                image, null, null, null, null,
                "fake.host.name", 0, UUID.randomUUID());

        this.vmInfo = new ProvisionVmInfo();
        this.vmInfo.vmId = this.vmId;
        this.vmInfo.image = image;
        this.vmInfo.mailRelayQuota = 5000;
        this.vmInfo.hasMonitoring = false;
        this.vmInfo.sgid = "";
        diskGib = new Random().nextInt(100);
        this.vmInfo.diskGib = diskGib;
        this.vmInfo.isManaged = false;

        request = new ProvisionRequest();
        request.rawFlavor = "";
        request.sgid = "";
        request.image_name = "";
        request.username = username;
        request.encryptedPassword = "sweeTT3st!".getBytes();
        request.zone = null;
        request.actionId = 12;
        request.vmInfo = vmInfo;
        shopperId = UUID.randomUUID().toString();
        request.shopperId = shopperId;
        request.serverName = expectedServerName;
        request.orionGuid = orionGuid;

        String messagedId = UUID.randomUUID().toString();
        when(messagingService.sendSetupEmail(anyString(), anyString(), anyString(), anyString(), anyBoolean()))
                .thenReturn(messagedId);

        hfsIp = new VmAddress();
        hfsIp.ip_address = "1.2.3.4";
        hfsVm = new Vm();
        hfsVm.address = hfsIp;
        hfsVm.resourceId = "test.resourceid.com";
        when(vmService.getVm(anyLong())).thenReturn(hfsVm);


        VmAction vmAction = new VmAction();
        vmAction.vmId = hfsVmId;
        vmAction.state = Status.COMPLETE;
        when(createVm.execute(any(CommandContext.class), any(CreateVm.Request.class))).thenReturn(vmAction);
        when(vmService.getVmAction(hfsVmId, vmAction.vmActionId)).thenReturn(vmAction);

        when(virtualMachineService.getVirtualMachine(vmInfo.vmId)).thenReturn(this.vm);

        when(context.execute(eq(CreateVm.class), any(CreateVm.Request.class))).thenReturn(vmAction);
    }

    @Test
    public void provisionVmTestUserHasAdminAccess() {
        this.image.controlPanel = ControlPanel.MYH;
        command.executeWithAction(context, this.request);
        verify(vmUserService, times(1)).updateUserAdminAccess(username, vmId, true);
    }

    @Test
    public void provisionVmTestUserDoesntHaveAdminAccess() {
        command.executeWithAction(context, this.request);
        verify(vmUserService, times(1)).updateUserAdminAccess(username, vmId, false);
    }

    @Test
    public void provisionVmTestPleskUserDoesntHaveAdminAccess() {
        this.image.controlPanel = ControlPanel.PLESK;
        command.executeWithAction(context, this.request);
        verify(vmUserService, times(1)).updateUserAdminAccess(username, vmId, false);
    }

    @Test
    public void testSendSetupEmail() {
        command.executeWithAction(context, this.request);
        verify(messagingService, times(1)).sendSetupEmail(shopperId, expectedServerName,
                hfsIp.ip_address, orionGuid.toString(), this.vmInfo.isManaged);
    }

    @Test
    public void testSendSetupEmailDoesNotThrowException() {
        when(messagingService.sendSetupEmail(anyString(), anyString(), anyString(), anyString(), anyBoolean()))
                .thenThrow(new RuntimeException("Unit test exception"));

        command.executeWithAction(context, this.request);
        verify(messagingService, times(1)).sendSetupEmail(anyString(), anyString(),
                anyString(), anyString(), anyBoolean());
    }

    @Test
    public void testSetEcommCommonName() {
        command.executeWithAction(context, this.request);
        verify(context, times(1))
                .execute(eq("SetCommonName"), setCommonNameLambdaCaptor.capture(), eq(Void.class));

        // Verify that the lambda is returning what we expect
        Function<CommandContext, Void> lambda = setCommonNameLambdaCaptor.getValue();
        lambda.apply(context);
        verify(creditService, times(1)).setCommonName(this.request.orionGuid, this.request.serverName);
    }

    @Test
    public void testSetsTheRootPasswordToBeSameAsUserPassword() {
        command.executeWithAction(context, this.request);
        verify(context, times(1))
                .execute(eq(SetPassword.class), setPasswordCaptor.capture());
        SetPassword.Request req = setPasswordCaptor.getValue();
        assertEquals(req.controlPanel, vm.image.getImageControlPanel());
        assertEquals(req.encryptedPassword, request.encryptedPassword);
        assertEquals(req.hfsVmId, hfsVmId);
    }

    @Test
    public void testSetsHostname() {
        command.executeWithAction(context, request);
        verify(context, times(1)).execute(eq(SetHostname.class), setHostnameArgumentCaptor.capture());
        SetHostname.Request req = setHostnameArgumentCaptor.getValue();
        assertEquals(req.controlPanel, request.vmInfo.image.getImageControlPanel());
        assertEquals(req.hfsVmId, hfsVmId);
        assertEquals(hfsVm.resourceId, req.hostname);
    }

    @Test
    public void testCreatePTRRecord() {
        command.executeWithAction(context, request);
        verify(context, times(1)).execute(eq("CreateDnsPtrRecord"), eq(CreateDnsPtrRecord.class), reverseDnsNameRequestCaptor.capture());
        CreateDnsPtrRecord.Request req = reverseDnsNameRequestCaptor.getValue();
        assertEquals("test.resourceid.com", req.reverseDnsName);
        assertEquals(this.vm, req.virtualMachine);
    }

    @Test
    public void updateHfsVmTrackingRecord() {
        command.executeWithAction(context, request);
        verify(context, times(1)).execute(eq("UpdateHfsVmTrackingRecord"),
                                          any(Function.class), eq(Void.class));
    }

    @Test
    public void provisionVmInvokesPanoptaSetup() {
        request.vmInfo.isPanoptaEnabled = true;
        when(virtualMachineService.getVirtualMachine(any(UUID.class))).thenReturn(vm);

        command.executeWithAction(context, this.request);
        verify(context, times(1)).execute(eq(SetupPanopta.class), setupPanoptaRequestArgCaptor.capture());
        SetupPanopta.Request capturedRequest = setupPanoptaRequestArgCaptor.getValue();
        assertEquals(capturedRequest.vmId, vmId);
        assertEquals(capturedRequest.hfsVmId, hfsVmId);
        assertEquals(capturedRequest.orionGuid, orionGuid);
        assertEquals(capturedRequest.shopperId, shopperId);
    }

    @Test
    public void provisionVMInvokesConfigurePanoptaAlert(){
        request.vmInfo.isPanoptaEnabled = true;
        request.vmInfo.hasMonitoring = true;
        command.executeWithAction(context, this.request);
        verify(vmAlertService).disableVmMetricAlert(request.vmInfo.vmId, VmMetric.FTP.name());
    }

    @Test
    public void provisionVMSkipsConfigurePanoptaAlert(){
        request.vmInfo.isPanoptaEnabled = true;
        request.vmInfo.hasMonitoring = false;
        command.executeWithAction(context, this.request);
        verify(vmAlertService, never()).disableVmMetricAlert(any(UUID.class), anyString());
    }

    @Test
    public void rebootWindowsServer() {
        this.vm.image.operatingSystem = Image.OperatingSystem.WINDOWS;
        command.executeWithAction(context, this.request);
        ArgumentCaptor<VmActionRequest> argument = ArgumentCaptor.forClass(VmActionRequest.class);
        verify(context).execute(eq(Vps4RebootDedicated.class), argument.capture());
        VmActionRequest actionRequest = argument.getValue();
        assertEquals(this.vm, actionRequest.virtualMachine);
    }

    @Test
    public void doesNotRebootLinuxServer() {
        command.executeWithAction(context, this.request);
        verify(context, never()).execute(eq(Vps4RebootDedicated.class), any());
    }
}
