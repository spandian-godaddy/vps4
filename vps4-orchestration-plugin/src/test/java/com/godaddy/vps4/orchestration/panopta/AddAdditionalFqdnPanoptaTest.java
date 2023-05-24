package com.godaddy.vps4.orchestration.panopta;

import com.godaddy.vps4.orchestration.TestCommandContext;
import com.godaddy.vps4.panopta.PanoptaDataService;
import com.godaddy.vps4.panopta.PanoptaServer;
import com.godaddy.vps4.panopta.PanoptaService;
import com.godaddy.vps4.panopta.PanoptaServiceException;
import com.godaddy.vps4.vm.Image;
import com.godaddy.vps4.vm.VmMetric;
import com.google.inject.Guice;
import com.google.inject.Injector;
import gdg.hfs.orchestration.CommandContext;
import gdg.hfs.orchestration.GuiceCommandProvider;
import org.junit.Before;
import org.junit.Test;
import java.util.UUID;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.internal.verification.VerificationModeFactory.times;

public class AddAdditionalFqdnPanoptaTest {

    PanoptaService panoptaService = mock(PanoptaService.class);
    PanoptaDataService panoptaDataService = mock(PanoptaDataService.class);
    AddAdditionalFqdnPanopta command = new AddAdditionalFqdnPanopta(panoptaDataService, panoptaService);

    Injector injector = Guice.createInjector(binder -> {
        binder.bind(AddAdditionalFqdnPanopta.class);
        binder.bind(PanoptaService.class).toInstance(panoptaService);
        binder.bind(PanoptaDataService.class).toInstance(panoptaDataService);
    });

    AddAdditionalFqdnPanopta.Request request;
    PanoptaServer panoptaServer;
    CommandContext context;
    UUID vmId = UUID.fromString("89111e35-6b2d-48c6-b293-d8cdb5207b19");

    @Before
    public void setUp() {
        request = new AddAdditionalFqdnPanopta.Request();
        request.additionalFqdn = "totally.fake.fqdn";
        request.isManaged = true;
        request.vmId = vmId;
        request.isHttps = false;
        request.operatingSystemId = Image.OperatingSystem.LINUX.getOperatingSystemId();
        panoptaServer = mock(PanoptaServer.class);
        panoptaServer.serverId = 1234567;
        when(panoptaService.getServer(vmId)).thenReturn(panoptaServer);
        context = new TestCommandContext(new GuiceCommandProvider(injector));
    }
    @Test
    public void testExecuteSuccess() throws PanoptaServiceException {
        UUID vmId = UUID.fromString("89111e35-6b2d-48c6-b293-d8cdb5207b19");
        command.execute(context, request);
        verify(panoptaService, times(1)).addAdditionalFqdnToServer(vmId, request.additionalFqdn);
        verify(panoptaService, times(1)).addNetworkService(request.vmId, VmMetric.HTTP_DOMAIN,
                request.additionalFqdn, request.operatingSystemId, request.isManaged);
        verify(panoptaDataService, times(1)).addPanoptaAdditionalFqdn(request.additionalFqdn, panoptaServer.serverId);
    }
}
