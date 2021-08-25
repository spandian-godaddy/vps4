package com.godaddy.vps4.orchestration.vm;

import static com.godaddy.vps4.credit.ECommCreditService.ProductMetaField.PLAN_CHANGE_PENDING;
import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyLong;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.Random;
import java.util.UUID;
import java.util.function.Function;

import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.MockitoAnnotations;

import com.godaddy.hfs.config.Config;
import com.godaddy.hfs.mailrelay.MailRelay;
import com.godaddy.hfs.mailrelay.MailRelayService;
import com.godaddy.hfs.mailrelay.MailRelayUpdate;
import com.godaddy.hfs.vm.Vm;
import com.godaddy.hfs.vm.VmAction;
import com.godaddy.hfs.vm.VmAction.Status;
import com.godaddy.hfs.vm.VmService;
import com.godaddy.vps4.credit.CreditService;
import com.godaddy.vps4.credit.VirtualMachineCredit;
import com.godaddy.vps4.hfs.HfsVmTrackingRecordService;
import com.godaddy.vps4.network.NetworkService;
import com.godaddy.vps4.orchestration.hfs.network.AllocateIp;
import com.godaddy.vps4.orchestration.hfs.sysadmin.SetHostname;
import com.godaddy.vps4.orchestration.hfs.sysadmin.SetPassword;
import com.godaddy.vps4.orchestration.hfs.vm.CreateVm;
import com.godaddy.vps4.orchestration.messaging.SendSetupCompletedEmail;
import com.godaddy.vps4.orchestration.messaging.SetupCompletedEmailRequest;
import com.godaddy.vps4.orchestration.panopta.SetupPanopta;
import com.godaddy.vps4.orchestration.scheduler.SetupAutomaticBackupSchedule;
import com.godaddy.vps4.orchestration.vm.provision.ProvisionRequest;
import com.godaddy.vps4.orchestration.vm.provision.Vps4ProvisionVm;
import com.godaddy.vps4.vm.ActionService;
import com.godaddy.vps4.vm.DataCenterService;
import com.godaddy.vps4.vm.Image;
import com.godaddy.vps4.vm.Image.ControlPanel;
import com.godaddy.vps4.vm.ProvisionVmInfo;
import com.godaddy.vps4.vm.VirtualMachine;
import com.godaddy.vps4.vm.VirtualMachineService;
import com.godaddy.vps4.vm.VmAlertService;
import com.godaddy.vps4.vm.VmMetric;
import com.godaddy.vps4.vm.VmUserService;

import gdg.hfs.orchestration.CommandContext;
import gdg.hfs.vhfs.network.IpAddress;

public class Vps4ProvisionVmTest {

    ActionService actionService = mock(ActionService.class);
    NetworkService networkService = mock(NetworkService.class);
    VirtualMachineService virtualMachineService = mock(VirtualMachineService.class);
    VmService vmService = mock(VmService.class);
    VmUserService vmUserService = mock(VmUserService.class);
    MailRelayService mailRelayService = mock(MailRelayService.class);
    AllocateIp allocateIp = mock(AllocateIp.class);
    SendSetupCompletedEmail sendSetupCompletedEmail = mock(SendSetupCompletedEmail.class);
    CreateVm createVm = mock(CreateVm.class);
    VirtualMachineCredit credit = mock(VirtualMachineCredit.class);
    CreditService creditService = mock(CreditService.class);
    Config config = mock(Config.class);
    HfsVmTrackingRecordService hfsVmTrackingRecordService = mock(HfsVmTrackingRecordService.class);
    VmAlertService vmAlertService = mock(VmAlertService.class);
    @Captor
    private ArgumentCaptor<Function<CommandContext, Void>> setCommonNameLambdaCaptor;
    @Captor
    private ArgumentCaptor<SetPassword.Request> setPasswordCaptor;
    @Captor
    private ArgumentCaptor<SetHostname.Request> setHostnameArgumentCaptor;
    @Captor
    private ArgumentCaptor<SetupPanopta.Request> setupPanoptaRequestArgCaptor;
    @Captor
    private ArgumentCaptor<SetupCompletedEmailRequest> setupCompletedEmailRequestArgCaptor;

    Vps4ProvisionVm command =
            new Vps4ProvisionVm(actionService, vmService, virtualMachineService, vmUserService, networkService,
                                 creditService, config, hfsVmTrackingRecordService, vmAlertService);

    CommandContext context = mock(CommandContext.class);

    VirtualMachine vm;
    ProvisionRequest request;
    IpAddress primaryIp;
    String expectedServerName;
    String username = "tester";
    UUID vmId = UUID.randomUUID();
    Image image;
    ProvisionVmInfo vmInfo;
    String shopperId;
    int diskGib;
    UUID orionGuid = UUID.randomUUID();
    long hfsVmId = 42;
    VmAction vmAction;
    Vm hfsVm;

    @Before
    public void setupTest() {
        MockitoAnnotations.initMocks(this);
        this.image = new Image();
        image.operatingSystem = Image.OperatingSystem.LINUX;
        image.controlPanel = ControlPanel.MYH;
        image.hfsName = "foobar";
        expectedServerName = "VM Name";
        this.vm = new VirtualMachine(UUID.randomUUID(), hfsVmId, UUID.randomUUID(), 1,
                                     null, expectedServerName,
                                     image, null, null, null, null, null,
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
        request.privateLabelId = "1";

        String messagedId = UUID.randomUUID().toString();
        when(sendSetupCompletedEmail.execute(any(CommandContext.class), any(SetupCompletedEmailRequest.class)))
                .thenReturn(messagedId);


        primaryIp = new IpAddress();
        primaryIp.address = "1.2.3.4";
        when(allocateIp.execute(any(CommandContext.class), any(AllocateIp.Request.class))).thenReturn(primaryIp);

        MailRelay relay = new MailRelay();
        relay.quota = vmInfo.mailRelayQuota;
        when(mailRelayService.setRelayQuota(eq(primaryIp.address), any(MailRelayUpdate.class))).thenReturn(relay);

        vmAction = new VmAction();
        vmAction.vmId = hfsVmId;
        vmAction.state = Status.COMPLETE;
        when(createVm.execute(any(CommandContext.class), any(CreateVm.Request.class))).thenReturn(vmAction);
        when(vmService.getVmAction(hfsVmId, vmAction.vmActionId)).thenReturn(vmAction);

        hfsVm = new Vm();
        hfsVm.resourceId = "somerandomresourceid";
        when(vmService.getVm(anyLong())).thenReturn(hfsVm);

        when(virtualMachineService.getVirtualMachine(vmInfo.vmId)).thenReturn(this.vm);

        when(creditService.getVirtualMachineCredit(orionGuid)).thenReturn(credit);

        when(context.execute(eq(AllocateIp.class), any(AllocateIp.Request.class))).thenReturn(primaryIp);
        when(context.execute(eq(SetupAutomaticBackupSchedule.class), any(SetupAutomaticBackupSchedule.Request.class)))
                .thenReturn(UUID.randomUUID());
        when(context.execute(eq(CreateVm.class), any(CreateVm.Request.class))).thenReturn(vmAction);
    }

    @Test
    public void provisionVmTestUserHasAdminAccess() {
        command.executeWithAction(context, this.request);
        verify(vmUserService, times(1)).updateUserAdminAccess(username, vmId, true);
    }

    @Test
    public void provisionVmTestUserDoesntHaveAdminAccess() {
        this.image.controlPanel = Image.ControlPanel.PLESK;
        command.executeWithAction(context, this.request);
        verify(vmUserService, times(1)).updateUserAdminAccess(username, vmId, false);
    }

    @Test
    public void provisionVmInvokesPanoptaSetup() {
        vm.primaryIpAddress = mock(com.godaddy.vps4.network.IpAddress.class);
        vm.primaryIpAddress.pingCheckId = 1234L;
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
    public void provisionCompletesEvenIfPanoptaInstallFails() {
        when(context.execute(eq(SetupPanopta.class), any(SetupPanopta.Request.class)))
                .thenThrow(new RuntimeException("Panopta broke"));
        command.executeWithAction(context, this.request);
        verify(actionService).updateActionState(request.actionId, "{\"step\":\"StartingServerSetup\"}");
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
    public void testSendSetupEmail() {
        command.executeWithAction(context, this.request);
        verify(context, times(1)).execute(eq(SendSetupCompletedEmail.class), setupCompletedEmailRequestArgCaptor.capture());
        SetupCompletedEmailRequest capturedRequest = setupCompletedEmailRequestArgCaptor.getValue();
        assertEquals(capturedRequest.serverName, expectedServerName);
        assertEquals(capturedRequest.ipAddress, primaryIp.address);
        assertEquals(capturedRequest.orionGuid, orionGuid);
        assertEquals(capturedRequest.shopperId, shopperId);
        assertEquals(capturedRequest.isManaged, vmInfo.isManaged);
    }

    @Test
    public void testSendSetupEmailDoesNotThrowException() {
        when(context.execute(eq(SendSetupCompletedEmail.class), any(SetupCompletedEmailRequest.class)))
                .thenThrow(new RuntimeException("SendMessageFailed"));
        command.executeWithAction(context, this.request);
        verify(context, times(1)).execute(eq(SendSetupCompletedEmail.class), setupCompletedEmailRequestArgCaptor.capture());
        verify(actionService).updateActionState(request.actionId, "{\"step\":\"SetupAutomaticBackupSchedule\"}");
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
    public void setsTheRootPasswordToBeSameAsUserPassword() {
        command.executeWithAction(context, this.request);
        verify(context, times(1))
                .execute(eq(SetPassword.class), setPasswordCaptor.capture());
        SetPassword.Request req = setPasswordCaptor.getValue();
        assertEquals(req.controlPanel, vm.image.getImageControlPanel());
        assertEquals(req.encryptedPassword, request.encryptedPassword);
        assertEquals(req.hfsVmId, hfsVmId);
    }

    @Test
    public void setsHostname() {
        command.executeWithAction(context, request);
        verify(context, times(1)).execute(eq(SetHostname.class), setHostnameArgumentCaptor.capture());
        SetHostname.Request req = setHostnameArgumentCaptor.getValue();
        assertEquals(req.hfsVmId, hfsVmId);
        String expectedHostname = "ip-" + primaryIp.address.replace('.', '-') + ".ip.secureserver.net";
        assertEquals(expectedHostname, req.hostname);
    }

    @Test
    public void updateHfsVmTrackingRecord() {
        command.executeWithAction(context, request);
        verify(context, times(1)).execute(eq("UpdateHfsVmTrackingRecord"),
                                          any(Function.class), eq(Void.class));
    }

    @Test
    public void rebootWindowsServer() {
        this.vm.image.operatingSystem = Image.OperatingSystem.WINDOWS;
        command.executeWithAction(context, this.request);
        ArgumentCaptor<VmActionRequest> argument = ArgumentCaptor.forClass(VmActionRequest.class);
        verify(context).execute(eq(Vps4RestartVm.class), argument.capture());
        VmActionRequest actionRequest = argument.getValue();
        assertEquals(this.vm, actionRequest.virtualMachine);
    }

    @Test
    public void doesNotRebootLinuxServer() {
        command.executeWithAction(context, this.request);
        verify(context, never()).execute(eq(Vps4RestartVm.class), any());
    }

    @Test
    public void validatePlanChangePending() {
        VirtualMachineCredit credit = new VirtualMachineCredit.Builder(mock(DataCenterService.class))
                .withProductMeta(Collections.singletonMap(PLAN_CHANGE_PENDING.toString(), "true"))
                .build();
        when(creditService.getVirtualMachineCredit(orionGuid)).thenReturn(credit);
        command.executeWithAction(context, request);
        verify(creditService, times(1)).updateProductMeta(orionGuid, PLAN_CHANGE_PENDING, "false");
    }
}
