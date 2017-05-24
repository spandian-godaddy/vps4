package com.godaddy.vps4.orchestration.sysadmin;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.UUID;

import org.junit.Before;
import org.junit.Test;

import com.godaddy.vps4.network.NetworkService;
import com.godaddy.vps4.orchestration.TestCommandContext;
import com.godaddy.vps4.orchestration.hfs.network.AllocateIp;
import com.godaddy.vps4.orchestration.hfs.network.BindIp;
import com.godaddy.vps4.orchestration.hfs.plesk.ConfigurePlesk;
import com.godaddy.vps4.orchestration.hfs.sysadmin.SetHostname;
import com.godaddy.vps4.orchestration.hfs.sysadmin.ToggleAdmin;
import com.godaddy.vps4.orchestration.hfs.vm.CreateVm;
import com.godaddy.vps4.orchestration.vm.Vps4ProvisionVm;
import com.godaddy.vps4.vm.AccountStatus;
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
import gdg.hfs.vhfs.mailrelay.MailRelay;
import gdg.hfs.vhfs.mailrelay.MailRelayService;
import gdg.hfs.vhfs.mailrelay.MailRelayUpdate;
import gdg.hfs.vhfs.network.IpAddress;
import gdg.hfs.vhfs.nodeping.NodePingService;
import gdg.hfs.vhfs.plesk.PleskService;
import gdg.hfs.vhfs.vm.CreateVMWithFlavorRequest;
import gdg.hfs.vhfs.vm.Vm;
import gdg.hfs.vhfs.vm.VmAction;
import gdg.hfs.vhfs.vm.VmService;

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

    Vps4ProvisionVm command = new Vps4ProvisionVm(actionService, vmService,
            virtualMachineService, vmUserService, networkService, mailRelayService, nodePingService);

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
    });

    CommandContext context = new TestCommandContext(new GuiceCommandProvider(injector));

    VirtualMachine vm;
    Vps4ProvisionVm.Request request;
    IpAddress primaryIp;
    MailRelayUpdate mrUpdate;
    String username = "tester";
    UUID vmId = UUID.randomUUID();
    Image image;

    @Before
    public void setupTest(){
        long hfsVmId = 42;
        this.image = new Image();
        image.operatingSystem = Image.OperatingSystem.WINDOWS;
        image.controlPanel = ControlPanel.MYH;
        this.vm = new VirtualMachine(UUID.randomUUID(), hfsVmId, UUID.randomUUID(), 1,
                null, "VM Name",
                image, null, null, null,
                "fake.host.name", AccountStatus.ACTIVE);


        CreateVMWithFlavorRequest hfsProvisionRequest = new CreateVMWithFlavorRequest();
        hfsProvisionRequest.rawFlavor = "";
        hfsProvisionRequest.sgid = "";
        hfsProvisionRequest.image_name = "";
        hfsProvisionRequest.username = this.username;
        hfsProvisionRequest.password = "sweeTT3st!";
        hfsProvisionRequest.zone = null;

        ProvisionVmInfo vmInfo = new ProvisionVmInfo();
        vmInfo.vmId = this.vmId;
        vmInfo.image = image;
        vmInfo.mailRelayQuota = 5000;
        vmInfo.pingCheckAccountId = 0;
        vmInfo.sgid = "";

        request = new Vps4ProvisionVm.Request();
        request.actionId = 12;
        request.hfsRequest = hfsProvisionRequest;
        request.vmInfo = vmInfo;

        IpAddress ip = new IpAddress();
        ip.address = "1.2.3.4";

        when(allocateIp.execute(any(CommandContext.class), any(AllocateIp.Request.class))).thenReturn(ip);

        MailRelay relay = new MailRelay();
        relay.quota = vmInfo.mailRelayQuota;
        when(mailRelayService.setRelayQuota(eq(ip.address), any(MailRelayUpdate.class))).thenReturn(relay);

        VmAction vmAction = new VmAction();
        vmAction.vmId = hfsVmId;
        when(createVm.execute(any(CommandContext.class), any(CreateVMWithFlavorRequest.class))).thenReturn(vmAction);
        Vm hfsVm = new Vm();
        hfsVm.vmId = hfsVmId;
        when(vmService.getVm(hfsVmId)).thenReturn(hfsVm);

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

}