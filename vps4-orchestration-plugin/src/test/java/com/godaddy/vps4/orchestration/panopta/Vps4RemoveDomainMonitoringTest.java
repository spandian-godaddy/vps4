package com.godaddy.vps4.orchestration.panopta;

import com.godaddy.vps4.panopta.PanoptaDataService;
import com.godaddy.vps4.panopta.PanoptaMetricId;
import com.godaddy.vps4.panopta.PanoptaServer;
import com.godaddy.vps4.panopta.PanoptaService;
import com.godaddy.vps4.panopta.PanoptaServiceException;
import com.godaddy.vps4.vm.ActionService;
import gdg.hfs.orchestration.CommandContext;
import org.junit.Before;
import org.junit.Test;

import java.util.UUID;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.internal.verification.VerificationModeFactory.times;

public class Vps4RemoveDomainMonitoringTest {
    ActionService actionService = mock(ActionService.class);
    PanoptaService panoptaService = mock(PanoptaService.class);
    PanoptaDataService panoptaDataService = mock(PanoptaDataService.class);
    Vps4RemoveDomainMonitoring command = new Vps4RemoveDomainMonitoring(actionService, panoptaDataService, panoptaService);
    CommandContext context = mock(CommandContext.class);

    UUID vmId = UUID.fromString("89111e35-6b2d-48c6-b293-d8cdb5207b19");

    Vps4RemoveDomainMonitoring.Request request;
    PanoptaServer panoptaServer;

    @Before
    public void setUp() throws PanoptaServiceException {
        when(context.getId()).thenReturn(UUID.randomUUID());
        request = new Vps4RemoveDomainMonitoring.Request();
        request.additionalFqdn = "totally.fake.fqdn";
        request.vmId = vmId;
        panoptaServer = mock(PanoptaServer.class);
        panoptaServer.serverId = 1234567;
        when(panoptaService.getServer(vmId)).thenReturn(panoptaServer);
        PanoptaMetricId fakeHTTPmetric = new PanoptaMetricId();
        fakeHTTPmetric.id = 123456;
        when(panoptaService.getNetworkIdOfAdditionalFqdn(vmId, request.additionalFqdn)).thenReturn(fakeHTTPmetric);
    }
    
    @Test
    public void testExecuteSuccess() throws PanoptaServiceException {
        command.execute(context, request);

        verify(panoptaService, times(1)).deleteAdditionalFqdnFromServer(vmId, request.additionalFqdn);
        verify(panoptaService, times(1)).deleteNetworkService(request.vmId, 123456);
        verify(panoptaDataService, times(1)).deletePanoptaAdditionalFqdn(request.additionalFqdn, panoptaServer.serverId);
    }
}
