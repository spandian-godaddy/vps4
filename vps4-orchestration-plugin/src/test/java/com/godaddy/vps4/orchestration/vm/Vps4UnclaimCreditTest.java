package com.godaddy.vps4.orchestration.vm;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.UUID;

import javax.ws.rs.NotFoundException;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import com.godaddy.hfs.mailrelay.MailRelay;
import com.godaddy.hfs.mailrelay.MailRelayService;
import com.godaddy.vps4.credit.CreditService;
import com.godaddy.vps4.credit.VirtualMachineCredit;
import com.godaddy.vps4.network.IpAddress;
import com.godaddy.vps4.vm.ServerSpec;
import com.godaddy.vps4.vm.ServerType;
import com.godaddy.vps4.vm.VirtualMachine;

import gdg.hfs.orchestration.CommandContext;

@RunWith(MockitoJUnitRunner.class)
public class Vps4UnclaimCreditTest {
    @Mock CommandContext context;
    @Mock CreditService creditService;
    @Mock MailRelayService mailRelayService;

    @Mock VirtualMachine vm;
    @Mock VirtualMachineCredit credit;

    Vps4UnclaimCredit command;

    @Before
    public void setUp() {
        vm.vmId = UUID.randomUUID();
        vm.orionGuid = UUID.randomUUID();
        vm.primaryIpAddress = new IpAddress();
        vm.primaryIpAddress.ipAddress = "127.0.0.1";
        vm.spec = mock(ServerSpec.class);
        vm.spec.serverType = mock(ServerType.class);
        vm.spec.serverType.serverType = ServerType.Type.VIRTUAL;
        MailRelay mailRelay = new MailRelay();
        mailRelay.relays = 2500;

        when(mailRelayService.getMailRelay("127.0.0.1")).thenReturn(mailRelay);
        when(credit.getProductId()).thenReturn(vm.vmId);
        when(creditService.getVirtualMachineCredit(vm.orionGuid)).thenReturn(credit);

        command = new Vps4UnclaimCredit(creditService, mailRelayService);
    }

    @Test
    public void unclaimsCredit() {
        command.execute(context, vm);
        verify(mailRelayService).getMailRelay("127.0.0.1");
        verify(creditService).unclaimVirtualMachineCredit(vm.orionGuid, vm.vmId, 2500);
    }

    @Test
    public void ignoresMailRelayIpNotFound() {
        when(mailRelayService.getMailRelay("127.0.0.1")).thenThrow(new NotFoundException("test"));
        command.execute(context, vm);
        verify(mailRelayService).getMailRelay("127.0.0.1");
        verify(creditService).unclaimVirtualMachineCredit(vm.orionGuid, vm.vmId, 0);
    }

    @Test
    public void skipsMailRelayForDedicated() {
        vm.spec.serverType.serverType = ServerType.Type.DEDICATED;
        command.execute(context, vm);
        verify(mailRelayService, never()).getMailRelay("127.0.0.1");
        verify(creditService).unclaimVirtualMachineCredit(vm.orionGuid, vm.vmId, 0);
    }

    @Test
    public void skipsMailRelayForEmptyPrimaryIp() {
        vm.primaryIpAddress = null;
        command.execute(context, vm);
        verify(mailRelayService, never()).getMailRelay("127.0.0.1");
        verify(creditService).unclaimVirtualMachineCredit(vm.orionGuid, vm.vmId, 0);
    }

    @Test
    public void onlyUnclaimCreditIfVmIdMatches() {
        when(credit.getProductId()).thenReturn(UUID.randomUUID());
        command.execute(context, vm);
        verify(mailRelayService, never()).getMailRelay("127.0.0.1");
        verify(creditService, never()).unclaimVirtualMachineCredit(vm.orionGuid, vm.vmId, 2500);
    }
}
