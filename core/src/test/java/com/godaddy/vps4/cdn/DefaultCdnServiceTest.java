package com.godaddy.vps4.cdn;

import com.godaddy.vps4.cdn.model.CdnBypassWAF;
import com.godaddy.vps4.cdn.model.CdnCacheLevel;
import com.godaddy.vps4.cdn.model.CdnClientInvalidateCacheResponse;
import com.godaddy.vps4.cdn.model.CdnClientInvalidateStatusResponse;
import com.godaddy.vps4.cdn.model.CdnClientUpdateRequest;
import com.godaddy.vps4.cdn.model.CdnDetail;
import com.godaddy.vps4.cdn.model.CdnClientCreateRequest;
import com.godaddy.vps4.cdn.model.CdnVerificationMethod;

import com.godaddy.vps4.cdn.model.CdnSite;
import com.godaddy.vps4.cdn.model.CdnStatus;
import com.godaddy.vps4.cdn.model.VmCdnSite;
import com.godaddy.vps4.network.IpAddress;
import com.godaddy.vps4.sso.Vps4SsoService;
import com.godaddy.vps4.sso.models.Vps4SsoToken;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.runners.MockitoJUnitRunner;

import javax.ws.rs.NotFoundException;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.anyString;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.eq;

@RunWith(MockitoJUnitRunner.class)
public class DefaultCdnServiceTest {
    private CdnClientService cdnClientService = mock(CdnClientService.class);
    private CdnDataService cdnDataService = mock(CdnDataService.class);
    private Vps4SsoService vps4SsoService = mock(Vps4SsoService.class);
    private CdnService service;
    @Captor
    private ArgumentCaptor<CdnClientUpdateRequest> modifyCdnArgumentCaptor;
    @Captor
    private ArgumentCaptor<CdnClientCreateRequest> createCdnArgumentCaptor;


    String shopperId = "12345678";
    String shopperJwt = "fakeShopperJwt";
    UUID vmId = UUID.randomUUID();
    Vps4SsoToken token;
    CdnSite cdnSite;
    VmCdnSite vmCdnSite;
    CdnDetail cdnSiteDetail;
    CdnClientInvalidateCacheResponse invalidateCacheResponse;
    CdnClientInvalidateStatusResponse invalidateStatusResponse;

    @Before
    public void setupTest() {
        service = new DefaultCdnService(cdnClientService, cdnDataService, vps4SsoService);

        cdnSite = new CdnSite();
        cdnSite.siteId = "fakeSiteId";
        cdnSite.domain = "fakeDomain.com";
        cdnSite.anyCastIP = "0.0.0.0";
        cdnSite.status = CdnStatus.SUCCESS;
        cdnSite.planId = "fakePlanId";

        cdnSiteDetail = new CdnDetail();
        cdnSiteDetail.siteId = "fakeSiteDetailId";

        vmCdnSite = new VmCdnSite();
        vmCdnSite.siteId = cdnSite.siteId;

        token = new Vps4SsoToken(1,"fakeMessage", shopperJwt);
        when(vps4SsoService.getDelegationToken("idp", shopperId)).thenReturn(token);
        when(cdnClientService.getCdnSites(anyString())).thenReturn(Collections.singletonList(cdnSite));
        when(cdnClientService.getCdnSiteDetail(anyString(), anyString())).thenReturn(cdnSiteDetail);
        when(cdnClientService.invalidateCdnCache(anyString(), anyString())).thenReturn(invalidateCacheResponse);
        when(cdnClientService.getCdnInvalidateStatus(anyString(), anyString(), anyString())).thenReturn(invalidateStatusResponse);

        when(cdnDataService.getCdnSiteFromId(eq(vmId), anyString())).thenReturn(vmCdnSite);
        when(cdnDataService.getActiveCdnSitesOfVm(eq(vmId))).thenReturn(Collections.singletonList(vmCdnSite));
    }

    @Test
    public void testGetCdnSitesNullCustomerJwt() {
        List<CdnSite> response = service.getCdnSites(shopperId, null, vmId);
        verify(vps4SsoService, times(1)).getDelegationToken("idp", shopperId);
        verify(cdnClientService, times(1)).getCdnSites("sso-jwt " + shopperJwt);

        assertEquals(1, response.size());
        assertSame(cdnSite, response.get(0));
    }

    @Test
    public void testGetCdnSitesWithCustomerJwt() {
        List<CdnSite> response = service.getCdnSites(shopperId, "customerJwt", vmId);
        verify(vps4SsoService, times(0)).getDelegationToken("idp", shopperId);
        verify(cdnClientService, times(1)).getCdnSites("sso-jwt customerJwt");

        assertEquals(1, response.size());
        assertSame(cdnSite, response.get(0));
    }

    @Test
    public void testGetCdnSitesDbEmptyList() {
        when(cdnDataService.getActiveCdnSitesOfVm(eq(vmId))).thenReturn(Collections.emptyList());

        List<CdnSite> response = service.getCdnSites(shopperId, "customerJwt", vmId);
        verify(vps4SsoService, times(0)).getDelegationToken("idp", shopperId);
        verify(cdnClientService, times(1)).getCdnSites("sso-jwt customerJwt");

        assertEquals(0, response.size());
    }

    @Test
    public void testGetCdnSitesFromListNoMatch() {
        VmCdnSite wrongCdnSite = new VmCdnSite();
        wrongCdnSite.siteId = "wrongSiteId";
        when(cdnDataService.getActiveCdnSitesOfVm(eq(vmId))).thenReturn(Collections.singletonList(wrongCdnSite));
        List<CdnSite> response = service.getCdnSites(shopperId, "customerJwt", vmId);
        verify(vps4SsoService, times(0)).getDelegationToken("idp", shopperId);
        verify(cdnClientService, times(1)).getCdnSites("sso-jwt customerJwt");

        assertEquals(0, response.size());
    }

    @Test
    public void testGetCdnSiteNullCustomerJwt() {
        CdnDetail response = service.getCdnSiteDetail(shopperId, null, cdnSiteDetail.siteId, vmId);
        verify(vps4SsoService, times(1)).getDelegationToken("idp", shopperId);
        verify(cdnClientService, times(1)).getCdnSiteDetail("sso-jwt " + shopperJwt, cdnSiteDetail.siteId);

        assertSame(cdnSiteDetail, response);
    }

    @Test
    public void testGetCdnSiteWithCustomerJwt() {
        CdnDetail response = service.getCdnSiteDetail(shopperId, "customerJwt", cdnSiteDetail.siteId, vmId);
        verify(vps4SsoService, times(0)).getDelegationToken("idp", shopperId);
        verify(cdnClientService, times(1)).getCdnSiteDetail("sso-jwt customerJwt", cdnSiteDetail.siteId);

        assertSame(cdnSiteDetail, response);
    }

    @Test(expected = NotFoundException.class)
    public void testGetCdnSiteDetailDbNotFoundThrowsException() {
        when(cdnDataService.getCdnSiteFromId(eq(vmId), anyString())).thenReturn(null);

        service.getCdnSiteDetail(shopperId, "customerJwt", cdnSiteDetail.siteId, vmId);
    }

    @Test
    public void testDeleteCdnSiteSuccessful() {
        service.deleteCdnSite(shopperId, null, cdnSiteDetail.siteId);
        verify(vps4SsoService, times(1)).getDelegationToken("idp", shopperId);
        verify(cdnClientService, times(1)).deleteCdnSite("sso-jwt " + shopperJwt, cdnSiteDetail.siteId);
    }

    @Test(expected = NotFoundException.class)
    public void testDeleteCdnSiteThrowsException() {
        when(cdnClientService.deleteCdnSite(anyString(), anyString())).thenThrow(new NotFoundException());

        service.deleteCdnSite(shopperId, null, cdnSiteDetail.siteId);
    }

    @Test
    public void testModifyCdnSiteSuccessful() {
        service.updateCdnSite(shopperId, null, cdnSiteDetail.siteId, CdnCacheLevel.CACHING_DISABLED, CdnBypassWAF.DISABLED);

        verify(vps4SsoService, times(1)).getDelegationToken("idp", shopperId);
        verify(cdnClientService, times(1))
                .modifyCdnSite(eq("sso-jwt " + shopperJwt), eq(cdnSiteDetail.siteId), modifyCdnArgumentCaptor.capture());

        CdnClientUpdateRequest req = modifyCdnArgumentCaptor.getValue();

        assertEquals(CdnBypassWAF.DISABLED.toString(), req.bypassWAF);
        assertEquals(CdnCacheLevel.CACHING_DISABLED.toString(), req.cacheLevel);
    }

    @Test
    public void testModifyCdnSiteNullBypassWAF() {
        service.updateCdnSite(shopperId, null, cdnSiteDetail.siteId, CdnCacheLevel.CACHING_DISABLED, null);
        verify(cdnClientService, times(1))
                .modifyCdnSite(eq("sso-jwt " + shopperJwt), eq(cdnSiteDetail.siteId), modifyCdnArgumentCaptor.capture());

        CdnClientUpdateRequest req = modifyCdnArgumentCaptor.getValue();

        assertEquals(null, req.bypassWAF);
        assertEquals(CdnCacheLevel.CACHING_DISABLED.toString(), req.cacheLevel);
    }

    @Test
    public void testModifyCdnSiteNullCacheLevel() {
        service.updateCdnSite(shopperId, null, cdnSiteDetail.siteId, null, CdnBypassWAF.DISABLED);
        verify(cdnClientService, times(1))
                .modifyCdnSite(eq("sso-jwt " + shopperJwt), eq(cdnSiteDetail.siteId), modifyCdnArgumentCaptor.capture());

        CdnClientUpdateRequest req = modifyCdnArgumentCaptor.getValue();

        assertEquals(CdnBypassWAF.DISABLED.toString(), req.bypassWAF);
        assertEquals(null, req.cacheLevel);
    }


    @Test(expected = NotFoundException.class)
    public void testModifyCdnSiteThrowsException() {
        when(cdnClientService.modifyCdnSite(anyString(), anyString(), any())).thenThrow(new NotFoundException());

        service.updateCdnSite(shopperId, null, cdnSiteDetail.siteId, null, CdnBypassWAF.DISABLED);
    }

    @Test
    public void testClearCdnSiteCacheSuccessful() {
        CdnClientInvalidateCacheResponse response = service.invalidateCdnCache(shopperId, null, cdnSiteDetail.siteId);
        verify(vps4SsoService, times(1)).getDelegationToken("idp", shopperId);
        verify(cdnClientService, times(1))
                .invalidateCdnCache(eq("sso-jwt " + shopperJwt), eq(cdnSiteDetail.siteId));

        assertSame(invalidateCacheResponse, response);
    }

    @Test
    public void testCreateCdnSiteSuccessful() {
        IpAddress address = new IpAddress();
        address.ipAddress = "fakeIpAddress";
        service.createCdn(shopperId, null, "fakedomain.com", address,
                CdnCacheLevel.CACHING_DISABLED.toString(), CdnBypassWAF.DISABLED.toString());
        verify(vps4SsoService, times(1)).getDelegationToken("idp", shopperId);
        verify(cdnClientService, times(1))
                .createCdnSite(eq("sso-jwt " + shopperJwt), createCdnArgumentCaptor.capture());

        CdnClientCreateRequest req = createCdnArgumentCaptor.getValue();

        assertEquals(CdnBypassWAF.DISABLED.toString(), req.bypassWAF);
        assertEquals(CdnCacheLevel.CACHING_DISABLED.toString(), req.cacheLevel);
        assertEquals(1, req.origins.length);
        assertEquals(1, req.autoMinify.length);

        assertEquals(address.ipAddress, req.origins[0].address);
        assertEquals("js", req.autoMinify[0]);
        assertEquals("fakedomain.com", req.domain);
        assertEquals("WSSWAFBasic", req.planId);
        assertEquals("CLOUDFLARE", req.provider);
        assertEquals("https", req.sslRedirect);
        assertEquals("off", req.imageOptimization);
        assertEquals(CdnVerificationMethod.TXT.toString(), req.verificationMethod);
        assertEquals(null, req.subdomains);
    }

    @Test
    public void testGetCdnInvalidateCacheStatusSuccessful() {
        CdnClientInvalidateStatusResponse response = service.getCdnInvalidateCacheStatus(shopperId, null, cdnSiteDetail.siteId, "fakeInvalidationId");
        verify(vps4SsoService, times(1)).getDelegationToken("idp", shopperId);
        verify(cdnClientService, times(1))
                .getCdnInvalidateStatus(eq("sso-jwt " + shopperJwt), eq(cdnSiteDetail.siteId), eq("fakeInvalidationId"));

        assertSame(invalidateStatusResponse, response);
    }
}
