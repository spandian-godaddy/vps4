package com.godaddy.vps4.orchestration.cdn;

import com.godaddy.vps4.cdn.CdnDataService;
import com.godaddy.vps4.cdn.CdnService;
import com.godaddy.vps4.cdn.model.CdnBypassWAF;
import com.godaddy.vps4.cdn.model.CdnCacheLevel;
import com.godaddy.vps4.cdn.model.CdnDetail;
import com.godaddy.vps4.cdn.model.CdnStatus;
import com.godaddy.vps4.cdn.model.VmCdnSite;
import com.godaddy.vps4.util.Cryptography;
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
    Cryptography cryptography = mock(Cryptography.class);
    Vps4ModifyCdnSite command = new Vps4ModifyCdnSite(actionService, cdnService, cryptography);
    CommandContext context = mock(CommandContext.class);

    UUID vmId = UUID.randomUUID();

    String encryptedJwtString = "encryptedJwt";
    String siteId = "fakeSiteId";

    String shopperId = "fakeShopperId";
    String decryptedJwtString = "decryptedJwt";

    byte[] encryptedJwt = encryptedJwtString.getBytes();
    Vps4ModifyCdnSite.Request request;
    CdnDetail cdnDetail;

    @Before
    public void setUp() {
        when(context.getId()).thenReturn(UUID.randomUUID());
        request = new Vps4ModifyCdnSite.Request();
        request.encryptedCustomerJwt = encryptedJwt;
        request.siteId = siteId;
        request.vmId = vmId;
        request.shopperId = shopperId;
        request.cacheLevel = CdnCacheLevel.CACHING_DISABLED;
        request.bypassWAF = CdnBypassWAF.DISABLED;
        cdnDetail = new CdnDetail();
        cdnDetail.siteId = siteId;
        cdnDetail.status = CdnStatus.SUCCESS;

        when(cryptography.decrypt(any())).thenReturn(decryptedJwtString);
        when(cdnService.getCdnSiteDetail(shopperId, decryptedJwtString, siteId, vmId, false)).thenReturn(cdnDetail);
    }
    
    @Test
    public void testExecuteSuccess() {
        command.execute(context, request);

        verify(cdnService, times(1)).getCdnSiteDetail(shopperId, decryptedJwtString, siteId, vmId, false);
        verify(cryptography, times(2)).decrypt(encryptedJwt);
        verify(cdnService, times(1))
                .updateCdnSite(shopperId, decryptedJwtString, siteId,
                        CdnCacheLevel.CACHING_DISABLED,
                        CdnBypassWAF.DISABLED);
    }

    @Test
    public void testExecuteNullCacheLevel() {
        request.cacheLevel = null;

        command.execute(context, request);

        verify(cdnService, times(1)).getCdnSiteDetail(shopperId, decryptedJwtString, siteId, vmId, false);
        verify(cryptography, times(2)).decrypt(encryptedJwt);
        verify(cdnService, times(1))
                .updateCdnSite(shopperId, decryptedJwtString, siteId,
                        null,
                        CdnBypassWAF.DISABLED);
    }

    @Test
    public void testExecuteNullBypassWAF() {
        request.bypassWAF = null;

        command.execute(context, request);

        verify(cdnService, times(1)).getCdnSiteDetail(shopperId, decryptedJwtString, siteId, vmId, false);
        verify(cryptography, times(2)).decrypt(encryptedJwt);
        verify(cdnService, times(1))
                .updateCdnSite(shopperId, decryptedJwtString, siteId,
                        CdnCacheLevel.CACHING_DISABLED,
                        null);
    }

    @Test
    public void testDoesNotCallCdnOnPendingStatus() {
        cdnDetail.status = CdnStatus.PENDING;
        when(cdnService.getCdnSiteDetail(shopperId, decryptedJwtString, siteId, vmId, false)).thenReturn(cdnDetail);

        command.execute(context, request);

        verify(cdnService, times(1)).getCdnSiteDetail(shopperId, decryptedJwtString, siteId, vmId, false);
        verify(cryptography, times(1)).decrypt(encryptedJwt);
        verify(cdnService, times(0))
                .updateCdnSite(anyString(), anyString(), anyString(),
                        any(),
                        any());
    }

    @Test
    public void testDoesNotCallCdnOnFailedStatus() {
        cdnDetail.status = CdnStatus.FAILED;
        when(cdnService.getCdnSiteDetail(shopperId, decryptedJwtString, siteId, vmId, false)).thenReturn(cdnDetail);

        command.execute(context, request);

        verify(cdnService, times(1)).getCdnSiteDetail(shopperId, decryptedJwtString, siteId, vmId, false);
        verify(cryptography, times(1)).decrypt(encryptedJwt);
        verify(cdnService, times(0))
                .updateCdnSite(anyString(), anyString(), anyString(),
                        any(),
                        any());
    }

    @Test(expected = RuntimeException.class)
    public void testExecuteCdnNotFoundThrowsException() {
        when(cdnService.getCdnSiteDetail(shopperId, decryptedJwtString, siteId, vmId, false)).thenThrow(new NotFoundException());
        command.execute(context, request);
    }
}
