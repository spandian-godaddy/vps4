package com.godaddy.vps4.orchestration.cdn;

import com.godaddy.vps4.cdn.CdnDataService;
import com.godaddy.vps4.cdn.CdnService;
import com.godaddy.vps4.cdn.model.CdnBypassWAF;
import com.godaddy.vps4.cdn.model.CdnCacheLevel;
import com.godaddy.vps4.cdn.model.VmCdnSite;
import com.godaddy.vps4.util.Cryptography;
import com.godaddy.vps4.vm.ActionService;
import gdg.hfs.orchestration.CommandContext;
import org.junit.Before;
import org.junit.Test;

import java.util.UUID;

import static org.mockito.Mockito.when;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.any;

import static org.mockito.internal.verification.VerificationModeFactory.times;

public class Vps4ModifyCdnSiteTest {
    ActionService actionService = mock(ActionService.class);
    CdnService cdnService = mock(CdnService.class);
    CdnDataService cdnDataService = mock(CdnDataService.class);
    Cryptography cryptography = mock(Cryptography.class);
    Vps4ModifyCdnSite command = new Vps4ModifyCdnSite(actionService, cdnDataService, cdnService, cryptography);
    CommandContext context = mock(CommandContext.class);

    UUID vmId = UUID.randomUUID();

    String encryptedJwtString = "encryptedJwt";
    String siteId = "fakeSiteId";

    String shopperId = "fakeShopperId";
    String decryptedJwtString = "decryptedJwt";

    byte[] encryptedJwt = encryptedJwtString.getBytes();
    Vps4ModifyCdnSite.Request request;
    VmCdnSite vmCdnSite;

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
        vmCdnSite = new VmCdnSite();
        vmCdnSite.siteId = siteId;
        vmCdnSite.vmId = vmId;

        when(cdnDataService.getCdnSiteFromId(vmId, siteId)).thenReturn(vmCdnSite);
        when(cryptography.decrypt(any())).thenReturn(decryptedJwtString);
    }
    
    @Test
    public void testExecuteSuccess() {
        command.execute(context, request);

        verify(cdnDataService, times(1)).getCdnSiteFromId(vmId, siteId);
        verify(cryptography, times(1)).decrypt(encryptedJwt);
        verify(cdnService, times(1))
                .updateCdnSite(shopperId, decryptedJwtString, siteId,
                        CdnCacheLevel.CACHING_DISABLED,
                        CdnBypassWAF.DISABLED);
    }

    @Test
    public void testExecuteNullCacheLevel() {
        request.cacheLevel = null;

        command.execute(context, request);

        verify(cdnDataService, times(1)).getCdnSiteFromId(vmId, siteId);
        verify(cryptography, times(1)).decrypt(encryptedJwt);
        verify(cdnService, times(1))
                .updateCdnSite(shopperId, decryptedJwtString, siteId,
                        null,
                        CdnBypassWAF.DISABLED);
    }

    @Test
    public void testExecuteNullBypassWAF() {
        request.bypassWAF = null;

        command.execute(context, request);

        verify(cdnDataService, times(1)).getCdnSiteFromId(vmId, siteId);
        verify(cryptography, times(1)).decrypt(encryptedJwt);
        verify(cdnService, times(1))
                .updateCdnSite(shopperId, decryptedJwtString, siteId,
                        CdnCacheLevel.CACHING_DISABLED,
                        null);
    }

    @Test(expected = RuntimeException.class)
    public void testExecuteCdnNotFound() {
        when(cdnDataService.getCdnSiteFromId(vmId, siteId)).thenReturn(null);
        command.execute(context, request);
    }
}
