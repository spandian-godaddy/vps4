package com.godaddy.vps4.orchestration.panopta;

import com.godaddy.vps4.panopta.PanoptaMetricId;
import com.godaddy.vps4.panopta.PanoptaServer;
import com.godaddy.vps4.panopta.PanoptaService;
import com.godaddy.vps4.panopta.PanoptaServiceException;
import com.godaddy.vps4.vm.ActionService;
import com.godaddy.vps4.vm.VmMetric;
import gdg.hfs.orchestration.CommandContext;
import org.junit.Before;
import org.junit.Test;

import java.util.UUID;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.internal.verification.VerificationModeFactory.times;

public class Vps4ReplaceDomainMonitoringTest {
    ActionService actionService = mock(ActionService.class);
    PanoptaService panoptaService = mock(PanoptaService.class);
    Vps4ReplaceDomainMonitoring command = new Vps4ReplaceDomainMonitoring(actionService, panoptaService);
    CommandContext context = mock(CommandContext.class);

    UUID vmId = UUID.fromString("89111e35-6b2d-48c6-b293-d8cdb5207b19");

    Vps4ReplaceDomainMonitoring.Request request;
    PanoptaServer panoptaServer;

    @Before
    public void setUp() throws PanoptaServiceException {
        when(context.getId()).thenReturn(UUID.randomUUID());
        request = new Vps4ReplaceDomainMonitoring.Request();
        request.additionalFqdn = "totally.fake.fqdn";
        request.vmId = vmId;;
        request.operatingSystemId = 1;;
        request.isManaged = true;
        request.protocol = "HTTP";
        panoptaServer = mock(PanoptaServer.class);
        panoptaServer.serverId = 1234567;
        when(panoptaService.getServer(vmId)).thenReturn(panoptaServer);
        PanoptaMetricId fakeHTTPSmetric = new PanoptaMetricId();
        fakeHTTPSmetric.id = 123456;
        fakeHTTPSmetric.typeId = 81;
        when(panoptaService.getNetworkIdOfAdditionalFqdn(vmId, request.additionalFqdn)).thenReturn(fakeHTTPSmetric);
    }
    
    @Test
    public void testExecuteSuccess() throws PanoptaServiceException {
        command.execute(context, request);
        verify(panoptaService, times(1)).deleteNetworkService(request.vmId, 123456);
        verify(panoptaService, times(1)).addNetworkService(request.vmId, VmMetric.HTTP,
                request.additionalFqdn,
                request.operatingSystemId,
                request.isManaged);
    }
}
