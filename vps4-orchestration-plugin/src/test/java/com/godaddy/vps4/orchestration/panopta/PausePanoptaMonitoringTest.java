package com.godaddy.vps4.orchestration.panopta;

import org.junit.Test;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.internal.verification.VerificationModeFactory.times;

import java.util.UUID;

import com.godaddy.vps4.orchestration.TestCommandContext;
import com.godaddy.vps4.panopta.PanoptaService;
import com.godaddy.vps4.panopta.PanoptaServiceException;
import com.google.inject.Guice;
import com.google.inject.Injector;

import gdg.hfs.orchestration.CommandContext;
import gdg.hfs.orchestration.GuiceCommandProvider;

public class PausePanoptaMonitoringTest {

    PanoptaService panoptaService = mock(PanoptaService.class);
    PausePanoptaMonitoring command = new PausePanoptaMonitoring(panoptaService);

    Injector injector = Guice.createInjector(binder -> {
        binder.bind(ResumePanoptaMonitoring.class);
        binder.bind(PanoptaService.class).toInstance(panoptaService);
    });

    CommandContext context = new TestCommandContext(new GuiceCommandProvider(injector));

    @Test
    public void testExecuteSuccess() throws PanoptaServiceException {
        UUID vmId = UUID.fromString("89111e35-6b2d-48c6-b293-d8cdb5207b19");
        command.execute(context, vmId);
        verify(panoptaService, times(1)).pauseServerMonitoring(vmId);
    }
}
