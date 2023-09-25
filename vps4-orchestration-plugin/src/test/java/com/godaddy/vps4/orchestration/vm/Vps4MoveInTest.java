package com.godaddy.vps4.orchestration.vm;

import com.godaddy.vps4.credit.CreditService;
import com.godaddy.vps4.orchestration.panopta.ApplyPanoptaTemplates;
import com.godaddy.vps4.orchestration.panopta.PausePanoptaMonitoring;
import com.godaddy.vps4.orchestration.panopta.ResumePanoptaMonitoring;
import com.godaddy.vps4.vm.ActionService;
import com.godaddy.vps4.vm.DataCenter;
import com.godaddy.vps4.vm.VirtualMachine;
import gdg.hfs.orchestration.CommandContext;
import org.junit.Before;
import org.junit.Test;

import java.util.UUID;
import java.util.function.Function;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class Vps4MoveInTest {
    private CommandContext context;
    private ActionService actionService;
    private CreditService creditService;
    private Vps4MoveIn command;

    private Vps4MoveIn.Request request;
    private VirtualMachine vm;

    @Before
    public void setup() {
        context = mock(CommandContext.class);
        actionService = mock(ActionService.class);
        creditService = mock(CreditService.class);
        command = new Vps4MoveIn(actionService, creditService);

        request = new Vps4MoveIn.Request();
        request.vm = mock(VirtualMachine.class);

        when(context.getId()).thenReturn(UUID.randomUUID());
    }

    @Test
    public void MoveInTest() {
        request.vm.orionGuid = UUID.randomUUID();
        request.vm.vmId = UUID.randomUUID();
        request.vm.dataCenter = new DataCenter();
        request.vm.dataCenter.dataCenterId = 1;

        command.execute(context, request);

        verify(context, times(1)).execute(eq("UpdateProdMeta"), any(Function.class), eq(Void.class));
        verify(context, times(1)).execute(eq(ResumePanoptaMonitoring.class), eq(request.vm));
        verify(context, times(1)).execute(eq("MoveInActions"), any(Function.class), eq(Void.class));
        verify(context, times(1)).execute(eq(ApplyPanoptaTemplates.class), any(ApplyPanoptaTemplates.Request.class));
    }
}
