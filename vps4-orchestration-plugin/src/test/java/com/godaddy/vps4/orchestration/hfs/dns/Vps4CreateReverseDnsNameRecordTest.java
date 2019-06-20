package com.godaddy.vps4.orchestration.hfs.dns;

import static org.mockito.Mockito.any;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.util.function.Function;

import org.junit.Test;

import com.godaddy.hfs.dns.HfsDnsAction;
import com.godaddy.hfs.dns.HfsDnsService;
import com.godaddy.vps4.vm.ActionService;
import com.godaddy.vps4.vm.VirtualMachine;

import gdg.hfs.orchestration.CommandContext;

public class Vps4CreateReverseDnsNameRecordTest {
    private HfsDnsService dnsService = mock(HfsDnsService.class);
    private ActionService actionService = mock(ActionService.class);
    private HfsDnsAction hfsDnsAction = mock(HfsDnsAction.class);

    private Vps4CreateReverseDnsNameRecord command = new Vps4CreateReverseDnsNameRecord(dnsService, actionService);
    private CommandContext commandContext = mock(CommandContext.class);


    @Test
    public void testExecuteWithActionSuccess() {
        long hfsVmId = 777L;
        String reverseDnsName = "fake_reverse_dns_name";
        VirtualMachine vm = mock(VirtualMachine.class);
        vm.hfsVmId = hfsVmId;
        Vps4ReverseDnsNameRecordRequest request = new Vps4ReverseDnsNameRecordRequest();
        request.actionId = hfsDnsAction.dns_action_id;
        request.virtualMachine = vm;
        request.reverseDnsName = reverseDnsName;

        command.executeWithAction(commandContext, request);

        verify(commandContext, times(1)).execute(eq("CreateReverseDnsNameRecord"), any(Function.class), eq(HfsDnsAction.class));
        verify(commandContext, times(1)).execute(eq(WaitForDnsAction.class), any(HfsDnsAction.class));
    }
}