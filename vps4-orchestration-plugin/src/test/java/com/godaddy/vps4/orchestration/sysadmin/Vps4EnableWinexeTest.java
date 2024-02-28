package com.godaddy.vps4.orchestration.sysadmin;

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Random;
import java.util.UUID;
import java.util.function.Function;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import com.godaddy.vps4.orchestration.vm.VmActionRequest;
import com.godaddy.vps4.vm.ActionService;
import com.godaddy.vps4.vm.VirtualMachine;

import gdg.hfs.orchestration.CommandContext;
import com.godaddy.hfs.sysadmin.SysAdminAction;
import com.godaddy.hfs.sysadmin.SysAdminService;

@RunWith(MockitoJUnitRunner.class)
public class Vps4EnableWinexeTest {
    private final long hfsVmId = new Random().nextLong();
    private Vps4EnableWinexe command;

    @Mock private CommandContext context;
    @Mock private ActionService actionService;
    @Mock private SysAdminService sysAdminService;

    @Mock private SysAdminAction action;
    @Mock private VirtualMachine vm;
    @Mock private VmActionRequest request;

    @Captor private ArgumentCaptor<Function<CommandContext, SysAdminAction>> enableWinexeCaptor;

    @Before
    public void setUp() throws Exception {
        action.vmId = hfsVmId;
        vm.hfsVmId = hfsVmId;
        request.virtualMachine = vm;
        when(context.getId()).thenReturn(UUID.randomUUID());
        when(sysAdminService.enableWinexe(hfsVmId)).thenReturn(action);
        command = new Vps4EnableWinexe(actionService, sysAdminService);
    }

    @Test
    public void callsHfsToEnableWinexe() {
        command.execute(context, request);

        verify(context, times(1)).execute(eq("EnableWinexe"),
                                          enableWinexeCaptor.capture(),
                                          eq(SysAdminAction.class));
        Function<CommandContext, SysAdminAction> lambdaValue = enableWinexeCaptor.getValue();
        SysAdminAction sysAdminAction = lambdaValue.apply(context);
        verify(sysAdminService, times(1)).enableWinexe(hfsVmId);
        assertEquals(hfsVmId, sysAdminAction.vmId);
    }
}
