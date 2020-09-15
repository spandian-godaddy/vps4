package com.godaddy.vps4.orchestration.vm;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import org.junit.Before;
import org.junit.Test;

public class Vps4DestroyOHVmTest extends Vps4DestroyVmTest {

    @Override
    @Before
    public void setUp() {
        super.setUp();
        // Run the same tests in Vps4DestroyVmTest but with Optimized Hosting VM destroy command
        command = new Vps4DestroyOHVm(actionService, networkService);
    }

    @Override
    @Test
    public void executesRemoveIp() {
        // Optimized Hosting VM destroys do not need to call RemoveIp command.
        command.execute(context, request);
        verify(context, never()).execute(anyString(), eq(Vps4RemoveIp.class), any());
    }
}
