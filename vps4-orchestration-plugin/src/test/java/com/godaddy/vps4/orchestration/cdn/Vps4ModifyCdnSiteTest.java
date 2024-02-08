package com.godaddy.vps4.orchestration.cdn;

import com.godaddy.vps4.cdn.CdnService;
import com.godaddy.vps4.cdn.model.CdnBypassWAF;
import com.godaddy.vps4.cdn.model.CdnCacheLevel;
import com.godaddy.vps4.cdn.model.CdnDetail;
import com.godaddy.vps4.cdn.model.CdnStatus;
import com.godaddy.vps4.vm.ActionService;
import gdg.hfs.orchestration.CommandContext;
import org.junit.Before;
import org.junit.Test;

import javax.ws.rs.NotFoundException;
import java.util.UUID;

import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.any;

import static org.mockito.internal.verification.VerificationModeFactory.times;

public class Vps4ModifyCdnSiteTest {
    ActionService actionService = mock(ActionService.class);
    CdnService cdnService = mock(CdnService.class);
    Vps4ModifyCdnSite command = new Vps4ModifyCdnSite(actionService, cdnService);
    CommandContext context = mock(CommandContext.class);

    UUID vmId = UUID.randomUUID();
    UUID customerId = UUID.randomUUID();
    String siteId = "fakeSiteId";
    Vps4ModifyCdnSite.Request request;
    CdnDetail cdnDetail;

    @Before
    public void setUp() {
        when(context.getId()).thenReturn(UUID.randomUUID());
        request = new Vps4ModifyCdnSite.Request();
        request.siteId = siteId;
        request.vmId = vmId;
        request.customerId = customerId;
        request.cacheLevel = CdnCacheLevel.CACHING_DISABLED;
        request.bypassWAF = CdnBypassWAF.DISABLED;
        cdnDetail = new CdnDetail();
        cdnDetail.siteId = siteId;
        cdnDetail.status = CdnStatus.SUCCESS;

        when(cdnService.getCdnSiteDetail(customerId, siteId, vmId, false)).thenReturn(cdnDetail);
    }
    
    @Test
    public void testExecuteSuccess() {
        command.execute(context, request);

        verify(cdnService, times(1)).getCdnSiteDetail(customerId, siteId, vmId, false);
        verify(cdnService, times(1))
                .updateCdnSite(customerId, siteId,
                        CdnCacheLevel.CACHING_DISABLED,
                        CdnBypassWAF.DISABLED);
    }

    @Test
    public void testExecuteNullCacheLevel() {
        request.cacheLevel = null;

        command.execute(context, request);

        verify(cdnService, times(1)).getCdnSiteDetail(customerId, siteId, vmId, false);
        verify(cdnService, times(1))
                .updateCdnSite(customerId, siteId,
                        null,
                        CdnBypassWAF.DISABLED);
    }

    @Test
    public void testExecuteNullBypassWAF() {
        request.bypassWAF = null;

        command.execute(context, request);

        verify(cdnService, times(1)).getCdnSiteDetail(customerId, siteId, vmId, false);
        verify(cdnService, times(1))
                .updateCdnSite(customerId, siteId,
                        CdnCacheLevel.CACHING_DISABLED,
                        null);
    }

    @Test
    public void testDoesNotCallCdnOnPendingStatus() {
        cdnDetail.status = CdnStatus.PENDING;
        when(cdnService.getCdnSiteDetail(customerId, siteId, vmId, false)).thenReturn(cdnDetail);

        command.execute(context, request);

        verify(cdnService, times(1)).getCdnSiteDetail(customerId, siteId, vmId, false);
        verify(cdnService, times(0))
                .updateCdnSite(any(), anyString(),
                        any(),
                        any());
    }

    @Test
    public void testDoesNotCallCdnOnFailedStatus() {
        cdnDetail.status = CdnStatus.FAILED;
        when(cdnService.getCdnSiteDetail(customerId, siteId, vmId, false)).thenReturn(cdnDetail);

        command.execute(context, request);

        verify(cdnService, times(1)).getCdnSiteDetail(customerId, siteId, vmId, false);
        verify(cdnService, times(0))
                .updateCdnSite(any(), anyString(),
                        any(),
                        any());
    }

    @Test(expected = RuntimeException.class)
    public void testExecuteCdnNotFoundThrowsException() {
        when(cdnService.getCdnSiteDetail(customerId, siteId, vmId, false)).thenThrow(new NotFoundException());
        command.execute(context, request);
    }
}
