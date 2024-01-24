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

public class Vps4DestroyOHVmTest extends Vps4DestroyVmTest {

    @Override
    @Before
    public void setUp() {
        super.setUp();
        // Run the same tests in Vps4DestroyVmTest but with Optimized Hosting VM destroy command
        command = new Vps4DestroyOHVm(actionService, networkService, shopperNotesService,
                                      snapshotService, virtualMachineService, cdnDataService);
    }

    @Override
    @Test
    public void doesNotRunDeleteAdditionalIpsIfThereAreNone() {
        when(networkService.getVmActiveSecondaryAddresses(vm.hfsVmId)).thenReturn(null);
        command.execute(context, request);
        verify(context, times(1)).execute(anyString(), eq(Vps4RemoveIp.class), any());
        verify(context, times(1)).execute(startsWith("MarkIpDeleted-"),
                                          Matchers.<Function<CommandContext, Void>> any(),
                                          eq(Void.class));
        verify(networkService, never()).destroyIpAddress(anyLong());
    }
}
