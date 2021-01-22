package com.godaddy.vps4.orchestration.panopta;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.UUID;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.runners.MockitoJUnitRunner;

import com.godaddy.vps4.panopta.PanoptaServer;
import com.godaddy.vps4.panopta.PanoptaService;

import gdg.hfs.orchestration.CommandContext;

@RunWith(MockitoJUnitRunner.class)
public class WaitForPanoptaSyncTest {
    private CommandContext context;
    private PanoptaService panoptaService;

    private WaitForPanoptaAgentSync command;

    private final UUID vmId = UUID.randomUUID();

    @Before
    public void setUp() throws Exception {
        context = mock(CommandContext.class);
        panoptaService = mock(PanoptaService.class);
        PanoptaServer server = mock(PanoptaServer.class);
        server.agentLastSynced = Instant.now();
        when(panoptaService.getServer(vmId)).thenReturn(server);
        command = new WaitForPanoptaAgentSync(panoptaService);
    }

    @Test
    public void returnsIfAgentIsSynced() {
        WaitForPanoptaAgentSync.Request request = new WaitForPanoptaAgentSync.Request();
        request.vmId = vmId;
        request.timeOfInstall = Instant.now().minusSeconds(30);
        command.execute(context, request);
        verify(panoptaService, times(1)).getServer(vmId);
    }
}
