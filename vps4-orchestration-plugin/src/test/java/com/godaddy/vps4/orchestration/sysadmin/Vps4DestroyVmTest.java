package com.godaddy.vps4.orchestration.sysadmin;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.UUID;

import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import com.godaddy.vps4.credit.CreditService;
import com.godaddy.vps4.network.IpAddress;
import com.godaddy.vps4.network.IpAddress.IpAddressType;
import com.godaddy.vps4.network.NetworkService;
import com.godaddy.vps4.orchestration.TestCommandContext;
import com.godaddy.vps4.orchestration.hfs.network.ReleaseIp;
import com.godaddy.vps4.orchestration.hfs.network.UnbindIp;
import com.godaddy.vps4.orchestration.vm.Vps4DestroyVm;
import com.godaddy.vps4.vm.AccountStatus;
import com.godaddy.vps4.vm.ActionService;
import com.godaddy.vps4.vm.VirtualMachine;
import com.godaddy.vps4.vm.VirtualMachineService;
import com.google.inject.Guice;
import com.google.inject.Injector;

import gdg.hfs.orchestration.CommandContext;
import gdg.hfs.orchestration.GuiceCommandProvider;
import gdg.hfs.vhfs.cpanel.CPanelService;
import gdg.hfs.vhfs.mailrelay.MailRelay;
import gdg.hfs.vhfs.mailrelay.MailRelayService;
import gdg.hfs.vhfs.mailrelay.MailRelayUpdate;
import gdg.hfs.vhfs.network.AddressAction;
import gdg.hfs.vhfs.nodeping.NodePingService;
import gdg.hfs.vhfs.plesk.PleskAction;
import gdg.hfs.vhfs.plesk.PleskService;
import gdg.hfs.vhfs.vm.VmAction;
import gdg.hfs.vhfs.vm.VmService;
import junit.framework.Assert;

public class Vps4DestroyVmTest {

    ActionService actionService = mock(ActionService.class);
    NetworkService networkService = mock(NetworkService.class);
    VirtualMachineService virtualMachineService = mock(VirtualMachineService.class);
    CreditService creditService = mock(CreditService.class);
    gdg.hfs.vhfs.network.NetworkService hfsNetworkService = mock(gdg.hfs.vhfs.network.NetworkService.class);
    CPanelService cpanelService = mock(CPanelService.class);
    PleskService pleskService = mock(PleskService.class);
    MailRelayService mailRelayService = mock(MailRelayService.class);
    VmService vmService = mock(VmService.class);
    NodePingService nodePingService = mock(NodePingService.class);

    Vps4DestroyVm command = new Vps4DestroyVm(actionService, networkService, virtualMachineService,
            creditService, vmService, cpanelService, nodePingService);

    Injector injector = Guice.createInjector(binder -> {
        binder.bind(UnbindIp.class);
        binder.bind(ReleaseIp.class);
        binder.bind(gdg.hfs.vhfs.network.NetworkService.class).toInstance(hfsNetworkService);
        binder.bind(VmService.class).toInstance(vmService);
        binder.bind(VirtualMachineService.class).toInstance(virtualMachineService);
        binder.bind(CPanelService.class).toInstance(cpanelService);
        binder.bind(PleskService.class).toInstance(pleskService);
        binder.bind(MailRelayService.class).toInstance(mailRelayService);
        binder.bind(NodePingService.class).toInstance(nodePingService);
    });

    CommandContext context = new TestCommandContext(new GuiceCommandProvider(injector));

    VirtualMachine vm;
    Vps4DestroyVm.Request request;
    IpAddress primaryIp;
    MailRelayUpdate mrUpdate;

    @Before
    public void setupTest(){
        this.vm = new VirtualMachine(UUID.randomUUID(), 42, UUID.randomUUID(), 1,
                null, "VM Name",
                null, null, null, null,
                "fake.host.name", AccountStatus.ACTIVE);

        request = new Vps4DestroyVm.Request();
        request.hfsVmId = 42;
        request.actionId = 12;
        request.pingCheckAccountId = 123;

        VmAction vmAction = new VmAction();
        vmAction.state = VmAction.Status.COMPLETE;

        AddressAction addressAction = new AddressAction();
        addressAction.status = AddressAction.Status.COMPLETE;

        this.primaryIp = new IpAddress(123, UUID.randomUUID(), "1.2.3.4", IpAddressType.PRIMARY, 5522L,
                Instant.now(), Instant.now().plus(24, ChronoUnit.HOURS));
        ArrayList<IpAddress> addresses = new ArrayList<IpAddress>();
        addresses.add(this.primaryIp);

        mrUpdate = new MailRelayUpdate();
        mrUpdate.quota = 0;

        when(virtualMachineService.getVirtualMachine(eq(this.request.hfsVmId))).thenReturn(this.vm);
        when(vmService.destroyVm(eq(this.request.hfsVmId))).thenReturn(vmAction);
        when(vmService.getVmAction(Mockito.anyLong(), Mockito.anyLong())).thenReturn(vmAction);
        when(networkService.getVmIpAddresses(this.vm.vmId)).thenReturn(addresses);
        when(hfsNetworkService.unbindIp(Mockito.anyLong())).thenReturn(addressAction);
        when(hfsNetworkService.releaseIp(Mockito.anyLong())).thenReturn(addressAction);
        doNothing().when(nodePingService).deleteCheck(request.pingCheckAccountId, primaryIp.pingCheckId);
    }

    @Test
    public void destroyVmSuccessPlesk() throws Exception {
        when(virtualMachineService.virtualMachineHasPlesk(this.vm.vmId)).thenReturn(true);
        PleskAction action = new PleskAction();
        action.status = PleskAction.Status.COMPLETE;
        when(pleskService.licenseRelease(this.vm.hfsVmId)).thenReturn(action);
        MailRelay mailRelay = new MailRelay();
        mailRelay.quota = 0;
        when(mailRelayService.setRelayQuota(eq("1.2.3.4"), any(MailRelayUpdate.class))).thenReturn(mailRelay);
        command.execute(context, this.request);
        verify(pleskService, times(1)).licenseRelease(this.request.hfsVmId);
        verify(nodePingService, times(1)).deleteCheck(request.pingCheckAccountId, primaryIp.pingCheckId);

        verifyMailRelay();

    }

    private void verifyMailRelay() {
        ArgumentCaptor<MailRelayUpdate> argument = ArgumentCaptor.forClass(MailRelayUpdate.class);
        ArgumentCaptor<String> ipAddress = ArgumentCaptor.forClass(String.class);
        verify(mailRelayService, times(1)).setRelayQuota(ipAddress.capture(), argument.capture());
        Assert.assertEquals(0, argument.getValue().quota);
        Assert.assertEquals("1.2.3.4", ipAddress.getValue());
    }
}
