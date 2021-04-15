package com.godaddy.vps4.orchestration.vm;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import org.junit.Before;
import org.junit.Test;

public class Vps4DestroyDedicatedTest extends Vps4DestroyVmTest {

    @Override
    @Before
    public void setUp() {
        super.setUp();
        // Run the same tests in Vps4DestroyVmTest but with Ded4 destroy command
        command = new Vps4DestroyDedicated(actionService, networkService);
    }

    @Override
    @Test
    public void executesRemoveIp() {
        // Ded4 boxes without secondary ips do not need to call RemoveIp command.
        command.execute(context, request);
        verify(context, never()).execute(anyString(), eq(Vps4RemoveIp.class), any());
    }
}
