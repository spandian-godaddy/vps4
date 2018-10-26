package com.godaddy.vps4.orchestration.vm;

import com.godaddy.vps4.credit.CreditService;
import com.godaddy.vps4.vm.*;
import com.google.inject.Guice;
import com.google.inject.Injector;
import gdg.hfs.orchestration.CommandContext;
import gdg.hfs.vhfs.vm.VmService;
import org.junit.Test;

import java.time.Instant;
import java.util.UUID;
import java.util.function.Function;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.*;

public class Vps4AbuseSuspendVmTest {
    ActionService actionService = mock(ActionService.class);
    VmService vmService = mock(VmService.class);
    CreditService creditService = mock(CreditService.class);

    Vps4AbuseSuspendVm command = new Vps4AbuseSuspendVm(actionService, vmService, creditService);

    Injector injector = Guice.createInjector(binder -> {
        binder.bind(ActionService.class).toInstance(actionService);
        binder.bind(VmService.class).toInstance(vmService);
        binder.bind(CreditService.class).toInstance(creditService);
    });

    CommandContext context = mock(CommandContext.class);

    @Test
    public void testAbuseSuspend() throws Exception {
        VmActionRequest request = new VmActionRequest();

        VirtualMachine virtualMachine = new VirtualMachine(UUID.randomUUID(),
                1111, UUID.randomUUID(), 0, null, "fakeName", null, null,
                Instant.now(), null, null, "fake.hostname.com", 0, UUID.randomUUID());
        request.virtualMachine = virtualMachine;

        VmAction inProgress = new VmAction();
        inProgress.id = 12l;
        inProgress.status = ActionStatus.IN_PROGRESS;
        when(context.execute(eq("Vps4StopVm"), (Function<CommandContext, Object>) any(), any())).thenReturn(new gdg.hfs.vhfs.vm.VmAction());
        when(context.execute(eq(WaitForManageVmAction.class), any())).thenReturn(new gdg.hfs.vhfs.vm.VmAction());

        command.executeWithAction(context, request);
        verify(creditService, times(1)).setStatus(virtualMachine.orionGuid, AccountStatus.ABUSE_SUSPENDED);
        verify(context, times(1)).execute(eq("Vps4StopVm"), (Function<CommandContext, Object>) any(), any());
    }


}
