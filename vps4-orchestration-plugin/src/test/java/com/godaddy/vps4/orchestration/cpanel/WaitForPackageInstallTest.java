package com.godaddy.vps4.orchestration.cpanel;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;

import com.godaddy.vps4.cpanel.CpanelAccessDeniedException;
import com.godaddy.vps4.cpanel.CpanelTimeoutException;
import com.godaddy.vps4.cpanel.Vps4CpanelService;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.runners.MockitoJUnitRunner;

import com.godaddy.vps4.panopta.PanoptaServer;

import gdg.hfs.orchestration.CommandContext;

@RunWith(MockitoJUnitRunner.class)
public class WaitForPackageInstallTest {
    private CommandContext context;
    private Vps4CpanelService cpanelService;

    private WaitForPackageInstall command;

    private final long hfsVmId = 23L;
    private final long buildNumber = 32L;

    @Before
    public void setUp() throws Exception {
        context = mock(CommandContext.class);
        cpanelService = mock(Vps4CpanelService.class);
        PanoptaServer server = mock(PanoptaServer.class);
        server.agentLastSynced = Instant.now();
        when(cpanelService.getActiveBuilds(hfsVmId, buildNumber)).thenReturn(0L);
        command = new WaitForPackageInstall(cpanelService);
    }

    @Test
    public void returnsIfPackageInstallFinishes() throws CpanelTimeoutException, CpanelAccessDeniedException {
        WaitForPackageInstall.Request request = new WaitForPackageInstall.Request();
        request.hfsVmId = hfsVmId;
        request.buildNumber = buildNumber;
        command.execute(context, request);
        verify(cpanelService, times(1)).getActiveBuilds(hfsVmId, buildNumber);
    }

    @Test(expected = RuntimeException.class)
    public void throwsExceptionIfNoBuildIsFound() throws CpanelTimeoutException, CpanelAccessDeniedException {
        when(cpanelService.getActiveBuilds(hfsVmId, buildNumber)).thenReturn(null);
        WaitForPackageInstall.Request request = new WaitForPackageInstall.Request();
        request.hfsVmId = hfsVmId;
        request.buildNumber = buildNumber;
        command.execute(context, request);
        verify(cpanelService, times(1)).getActiveBuilds(hfsVmId, buildNumber);
    }
}
