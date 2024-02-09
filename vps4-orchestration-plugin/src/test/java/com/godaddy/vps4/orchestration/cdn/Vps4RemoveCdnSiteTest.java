package com.godaddy.vps4.orchestration.cdn;

import com.godaddy.vps4.cdn.CdnDataService;
import com.godaddy.vps4.cdn.CdnService;
import com.godaddy.vps4.cdn.model.VmCdnSite;
import com.godaddy.vps4.vm.ActionService;
import gdg.hfs.orchestration.CommandContext;
import org.junit.Before;
import org.junit.Test;

import java.util.UUID;

import static org.mockito.Mockito.when;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import static org.mockito.internal.verification.VerificationModeFactory.times;

public class Vps4RemoveCdnSiteTest {
    ActionService actionService = mock(ActionService.class);
    CdnService cdnService = mock(CdnService.class);
    CdnDataService cdnDataService = mock(CdnDataService.class);
    Vps4RemoveCdnSite command = new Vps4RemoveCdnSite(actionService, cdnDataService, cdnService);
    CommandContext context = mock(CommandContext.class);

    UUID vmId = UUID.randomUUID();
    UUID customerId = UUID.randomUUID();
    String siteId = "fakeSiteId";
    Vps4RemoveCdnSite.Request request;
    VmCdnSite vmCdnSite;

    @Before
    public void setUp() {
        when(context.getId()).thenReturn(UUID.randomUUID());
        request = new Vps4RemoveCdnSite.Request();
        request.customerId = customerId;
        request.siteId = siteId;
        request.vmId = vmId;
        vmCdnSite = new VmCdnSite();
        vmCdnSite.siteId = siteId;
        vmCdnSite.vmId = vmId;

        when(cdnDataService.getCdnSiteFromId(vmId, siteId)).thenReturn(vmCdnSite);
    }
    
    @Test
    public void testExecuteSuccess() {
        command.execute(context, request);

        verify(cdnDataService, times(1)).getCdnSiteFromId(vmId, siteId);
        verify(cdnService, times(1)).deleteCdnSite(customerId, siteId);
        verify(cdnDataService, times(1)).destroyCdnSite(vmId, siteId);
    }

    @Test(expected = RuntimeException.class)
    public void testExecuteCdnNotFound() {
        when(cdnDataService.getCdnSiteFromId(vmId, siteId)).thenReturn(null);
        command.execute(context, request);
    }
}
