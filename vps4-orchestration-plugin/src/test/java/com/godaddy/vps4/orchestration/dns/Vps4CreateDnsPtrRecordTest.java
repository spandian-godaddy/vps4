package com.godaddy.vps4.orchestration.dns;

import static org.mockito.Mockito.any;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import org.junit.Test;

import com.godaddy.hfs.dns.HfsDnsAction;
import com.godaddy.vps4.orchestration.hfs.dns.CreateDnsPtrRecord;
import com.godaddy.vps4.vm.ActionService;
import com.godaddy.vps4.vm.VirtualMachine;

import gdg.hfs.orchestration.CommandContext;

public class Vps4CreateDnsPtrRecordTest {
    private ActionService actionService = mock(ActionService.class);
    private HfsDnsAction hfsDnsAction = mock(HfsDnsAction.class);

    private Vps4CreateDnsPtrRecord command = new Vps4CreateDnsPtrRecord(actionService);
    private CommandContext commandContext = mock(CommandContext.class);


    @Test
    public void testExecuteWithActionSuccess() {
        long hfsVmId = 777L;
        String reverseDnsName = "fake_reverse_dns_name";
        VirtualMachine vm = mock(VirtualMachine.class);
        vm.hfsVmId = hfsVmId;
        Vps4CreateDnsPtrRecord.Request request = new Vps4CreateDnsPtrRecord.Request();
        request.actionId = hfsDnsAction.dns_action_id;
        request.virtualMachine = vm;
        request.reverseDnsName = reverseDnsName;
        command.executeWithAction(commandContext, request);
        verify(commandContext, times(1)).execute(eq(CreateDnsPtrRecord.class), any(CreateDnsPtrRecord.Request.class));
    }
}