package com.godaddy.vps4.orchestration.vm;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyLong;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Matchers.startsWith;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.function.Function;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Matchers;

import gdg.hfs.orchestration.CommandContext;

public class Vps4DestroyDedicatedTest extends Vps4DestroyVmTest {

    @Override
    @Before
    public void setUp() {
        super.setUp();
        // Run the same tests in Vps4DestroyVmTest but with Ded4 destroy command
        command = new Vps4DestroyDedicated(actionService, networkService, shopperNotesService,
                                           snapshotService, virtualMachineService);
    }

    @Override
    @Test
    public void executesRemoveIp() {
        // Ded4 boxes without secondary ips do not need to call RemoveIp command.
        command.execute(context, request);
        verify(context, never()).execute(anyString(), eq(Vps4RemoveIp.class), any());
    }

    @Override
    @Test
    public void doesNotRunDeleteAdditionalIpsIfThereAreNone() {
        when(networkService.getVmActiveSecondaryAddresses(vm.hfsVmId)).thenReturn(null);
        command.execute(context, request);
        verify(context, never()).execute(anyString(), eq(Vps4RemoveIp.class), any());
        verify(context, times(1)).execute(startsWith("MarkIpDeleted-"),
                                          Matchers.<Function<CommandContext, Void>> any(),
                                          eq(Void.class));
        verify(networkService, never()).destroyIpAddress(anyLong());
    }
}
