package com.godaddy.vps4.orchestration.vm;

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

import java.io.IOException;
import java.util.Random;
import java.util.UUID;

import com.godaddy.hfs.config.Config;
import com.godaddy.vps4.orchestration.sysadmin.ConfigureMailRelay;
import com.godaddy.vps4.orchestration.vm.provision.ProvisionRequest;
import org.junit.Before;
import org.junit.Test;

import com.godaddy.vps4.credit.CreditService;
import com.godaddy.vps4.messaging.MissingShopperIdException;
import com.godaddy.vps4.messaging.Vps4MessagingService;
import com.godaddy.vps4.network.NetworkService;
import com.godaddy.vps4.orchestration.TestCommandContext;
import com.godaddy.vps4.orchestration.hfs.network.AllocateIp;
import com.godaddy.vps4.orchestration.hfs.network.BindIp;
import com.godaddy.vps4.orchestration.hfs.plesk.ConfigurePlesk;
import com.godaddy.vps4.orchestration.hfs.sysadmin.SetHostname;
import com.godaddy.vps4.orchestration.hfs.sysadmin.ToggleAdmin;
import com.godaddy.vps4.orchestration.hfs.vm.CreateVm;
import com.godaddy.vps4.orchestration.vm.provision.Vps4ProvisionVm;
import com.godaddy.vps4.util.MonitoringMeta;
import com.godaddy.vps4.vm.ActionService;
import com.godaddy.vps4.vm.Image;
import com.godaddy.vps4.vm.Image.ControlPanel;
import com.godaddy.vps4.vm.ProvisionVmInfo;
import com.godaddy.vps4.vm.VirtualMachine;
import com.godaddy.vps4.vm.VirtualMachineService;
import com.godaddy.vps4.vm.VmUserService;
import com.google.inject.Guice;
import com.google.inject.Injector;

import gdg.hfs.orchestration.CommandContext;
import gdg.hfs.orchestration.GuiceCommandProvider;
import com.godaddy.hfs.mailrelay.MailRelay;
import com.godaddy.hfs.mailrelay.MailRelayService;
import com.godaddy.hfs.mailrelay.MailRelayUpdate;
import gdg.hfs.vhfs.network.IpAddress;
import gdg.hfs.vhfs.nodeping.CreateCheckRequest;
import gdg.hfs.vhfs.nodeping.NodePingCheck;
import gdg.hfs.vhfs.nodeping.NodePingService;
import gdg.hfs.vhfs.plesk.PleskService;
import com.godaddy.hfs.vm.VmAction;
import com.godaddy.hfs.vm.VmAction.Status;
import com.godaddy.hfs.vm.VmService;

public class Vps4ProvisionVmTest {

    ActionService actionService = mock(ActionService.class);
    NetworkService networkService = mock(NetworkService.class);
    VirtualMachineService virtualMachineService = mock(VirtualMachineService.class);
    VmService vmService = mock(VmService.class);
    VmUserService vmUserService = mock(VmUserService.class);
    MailRelayService mailRelayService = mock(MailRelayService.class);
    AllocateIp allocateIp = mock(AllocateIp.class);
    CreateVm createVm = mock(CreateVm.class);
    BindIp bindIp = mock(BindIp.class);
    SetHostname setHostname = mock(SetHostname.class);
    ToggleAdmin toggleAdmin = mock(ToggleAdmin.class);
    ConfigureMailRelay configureMailRelay = mock(ConfigureMailRelay.class);
    NodePingService nodePingService = mock(NodePingService.class);
    MonitoringMeta monitoringMeta = mock(MonitoringMeta.class);
    Vps4MessagingService messagingService = mock(Vps4MessagingService.class);
    CreditService creditService = mock(CreditService.class);
    Config config = mock(Config.class);

    Vps4ProvisionVm command = new Vps4ProvisionVm(actionService, vmService,
            virtualMachineService, vmUserService, networkService, nodePingService,
            monitoringMeta, messagingService, creditService, config);

    Injector injector = Guice.createInjector(binder -> {
        binder.bind(ActionService.class).toInstance(actionService);
        binder.bind(NetworkService.class).toInstance(networkService);
        binder.bind(VmService.class).toInstance(vmService);
        binder.bind(VirtualMachineService.class).toInstance(virtualMachineService);
        binder.bind(MailRelayService.class).toInstance(mailRelayService);
        binder.bind(VmUserService.class).toInstance(vmUserService);
        binder.bind(AllocateIp.class).toInstance(allocateIp);
        binder.bind(CreateVm.class).toInstance(createVm);
        binder.bind(BindIp.class).toInstance(bindIp);
        binder.bind(SetHostname.class).toInstance(setHostname);
        binder.bind(ToggleAdmin.class).toInstance(toggleAdmin);
        binder.bind(ConfigureMailRelay.class).toInstance(configureMailRelay);
        binder.bind(PleskService.class).toInstance(mock(PleskService.class));
        binder.bind(ConfigurePlesk.class).toInstance(mock(ConfigurePlesk.class));
        binder.bind(Vps4MessagingService.class).toInstance(messagingService);
        binder.bind(CreditService.class).toInstance(creditService);
        binder.bind(Config.class).toInstance(config);
    });

    CommandContext context = new TestCommandContext(new GuiceCommandProvider(injector));

    VirtualMachine vm;
    ProvisionRequest request;
    IpAddress primaryIp;
    String expectedServerName;
    MailRelayUpdate mrUpdate;
    String username = "tester";
    UUID vmId = UUID.randomUUID();
    Image image;
    ProvisionVmInfo vmInfo;
    String shopperId;
    int diskGib;
    UUID orionGuid = UUID.randomUUID();

    @Before
    public void setupTest() throws Exception {
        long hfsVmId = 42;
        this.image = new Image();
        image.operatingSystem = Image.OperatingSystem.WINDOWS;
        image.controlPanel = ControlPanel.MYH;
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
        this.vmInfo.managedLevel = 0;

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

        primaryIp = new IpAddress();
        primaryIp.address = "1.2.3.4";
        when(allocateIp.execute(any(CommandContext.class), any(AllocateIp.Request.class))).thenReturn(primaryIp);

        MailRelay relay = new MailRelay();
        relay.quota = vmInfo.mailRelayQuota;
        when(mailRelayService.setRelayQuota(eq(primaryIp.address), any(MailRelayUpdate.class))).thenReturn(relay);

        VmAction vmAction = new VmAction();
        vmAction.vmId = hfsVmId;
        vmAction.state = Status.COMPLETE;
        when(createVm.execute(any(CommandContext.class), any(CreateVm.Request.class))).thenReturn(vmAction);
        when(vmService.getVmAction(hfsVmId, vmAction.vmActionId)).thenReturn(vmAction);

        when(virtualMachineService.getVirtualMachine(vmInfo.vmId)).thenReturn(this.vm);
    }

    @Test
    public void provisionVmTestUserHasAdminAccess() throws Exception {
        command.execute(context, this.request);
        verify(vmUserService, times(1)).updateUserAdminAccess(username, vmId, true);
    }

    @Test
    public void provisionVmTestUserDoesntHaveAdminAccess() throws Exception {
        this.image.controlPanel = Image.ControlPanel.PLESK;
        command.execute(context, this.request);
        verify(vmUserService, times(1)).updateUserAdminAccess(username, vmId, false);
    }

    @Test
    public void testProvisionVmDoesntConfigureNodePing() {
        command.execute(context, this.request);
        verify(nodePingService, never()).createCheck(anyLong(), any(CreateCheckRequest.class));
    }

    @Test
    public void testProvisionVmConfiguresNodePing() {
        NodePingCheck check = mock(NodePingCheck.class);
        check.checkId = 1;
        when(nodePingService.createCheck(anyLong(), any())).thenReturn(check);
        when(monitoringMeta.getAccountId()).thenReturn(1L);
        when(monitoringMeta.getGeoRegion()).thenReturn("nam");
        this.vmInfo.hasMonitoring = true;

        command.execute(context, this.request);
        verify(nodePingService, times(1)).createCheck(eq(1L), any(CreateCheckRequest.class));
    }

    @Test
    public void testSendSetupEmail() throws MissingShopperIdException, IOException {
        command.execute(context, this.request);
        verify(messagingService, times(1)).sendSetupEmail(shopperId, expectedServerName,
                primaryIp.address, orionGuid.toString(), this.vmInfo.isFullyManaged());
    }

    @Test
    public void testSendSetupEmailDoesNotThrowException() throws MissingShopperIdException, IOException {
        when(messagingService.sendSetupEmail(anyString(), anyString(), anyString(), anyString(), anyBoolean()))
                .thenThrow(new RuntimeException("Unit test exception"));

        command.execute(context, this.request);
        verify(messagingService, times(1)).sendSetupEmail(anyString(), anyString(),
                anyString(), anyString(), anyBoolean());
    }

    @Test
    public void testSetEcommCommonName() {
        command.execute(context, this.request);
        verify(creditService, times(1)).setCommonName(this.request.orionGuid, this.request.serverName);
    }
}
