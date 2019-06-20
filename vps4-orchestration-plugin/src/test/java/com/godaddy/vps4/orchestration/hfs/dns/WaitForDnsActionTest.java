package com.godaddy.vps4.orchestration.hfs.dns;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.Test;

import com.godaddy.hfs.dns.HfsDnsAction;
import com.godaddy.hfs.dns.HfsDnsService;
import com.godaddy.vps4.vm.ActionStatus;

import gdg.hfs.orchestration.CommandContext;

public class WaitForDnsActionTest {
    private HfsDnsService hfsDnsService = mock(HfsDnsService.class);
    private CommandContext context = mock(CommandContext.class);

    private WaitForDnsAction command = new WaitForDnsAction(hfsDnsService);
    private long hfsVmId = 777L;
    private long dnsActionId = 123;
    private long sleepTime = 2000;
    private String reverseDnsName = "fake_reverse_dns_name";

    private HfsDnsAction createMockDnsAction(ActionStatus status) {
        HfsDnsAction mockAction = mock(HfsDnsAction.class);
        mockAction.vm_id = hfsVmId;
        mockAction.dns_action_id = dnsActionId;
        mockAction.status = status;
        return mockAction;
    }

    @Test
    public void testExecuteWaitForComplete() {
        HfsDnsAction newHfsAction = createMockDnsAction(ActionStatus.NEW);
        HfsDnsAction inProgressHfsAction = createMockDnsAction(ActionStatus.IN_PROGRESS);
        HfsDnsAction completeHfsAction = createMockDnsAction(ActionStatus.COMPLETE);
        // simulates the hfs dns action progressing through states, new->in_progress->complete
        when(hfsDnsService.getDnsAction(dnsActionId)).thenReturn(newHfsAction).thenReturn(inProgressHfsAction)
                                                     .thenReturn(completeHfsAction);

        HfsDnsAction result = command.execute(context, newHfsAction);

        verify(hfsDnsService, times(3)).getDnsAction(dnsActionId);
        verify(context, times(3)).sleep(sleepTime);
        assertEquals(completeHfsAction, result);
    }

    @Test(expected = RuntimeException.class)
    public void testExecuteActionFails() {
        HfsDnsAction newHfsAction = createMockDnsAction(ActionStatus.NEW);
        HfsDnsAction errorHfsAction = createMockDnsAction(ActionStatus.ERROR);
        when(hfsDnsService.createReverseDnsNameRecord(hfsVmId, reverseDnsName)).thenReturn(errorHfsAction);

        command.execute(context, newHfsAction);
    }
}