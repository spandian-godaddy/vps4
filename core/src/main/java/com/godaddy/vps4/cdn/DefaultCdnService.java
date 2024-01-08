package com.godaddy.vps4.cdn;

import com.godaddy.vps4.cdn.model.CdnClientCreateRequest;
import com.godaddy.vps4.cdn.model.CdnClientCreateResponse;
import com.godaddy.vps4.cdn.model.CdnClientInvalidateCacheResponse;
import com.godaddy.vps4.cdn.model.CdnClientInvalidateStatusResponse;
import com.godaddy.vps4.cdn.model.CdnClientUpdateRequest;
import com.godaddy.vps4.cdn.model.CdnDetail;
import com.godaddy.vps4.cdn.model.CdnOrigin;
import com.godaddy.vps4.cdn.model.CdnSite;
import com.godaddy.vps4.cdn.model.VmCdnSite;
import com.godaddy.vps4.cdn.model.CdnBypassWAF;
import com.godaddy.vps4.cdn.model.CdnCacheLevel;
import com.godaddy.vps4.cdn.model.CdnVerificationMethod;
import com.godaddy.vps4.network.IpAddress;
import com.godaddy.vps4.sso.Vps4SsoService;
import com.godaddy.vps4.sso.models.Vps4SsoToken;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.ws.rs.NotFoundException;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

public class DefaultCdnService implements CdnService {
    private static final String CDN_PLAN_ID = "WSSWAFBasic";
    private static final String CDN_CLOUDFLARE_PROVIDER = "CLOUDFLARE";
    private static final Logger logger = LoggerFactory.getLogger(DefaultCdnService.class);

    private final Vps4SsoService ssoService;
    private final CdnClientService cdnClientService;
    private final CdnDataService cdnDataService;

    @Inject
    public DefaultCdnService(CdnClientService cdnClientService,
                             CdnDataService cdnDataService,
                             Vps4SsoService ssoService)
    {
        this.cdnClientService = cdnClientService;
        this.cdnDataService = cdnDataService;
        this.ssoService = ssoService;
    }

    protected String getAuthToken(String shopperId, String customerJwt) {
        String ssoTokenHeader = "sso-jwt ";
        if (customerJwt == null) {
            Vps4SsoToken token = ssoService.getDelegationToken("idp", shopperId);
            ssoTokenHeader += token.data;
        } else {
            ssoTokenHeader += customerJwt;
        }
        return ssoTokenHeader;
    }

    @Override
    public List<CdnSite> getCdnSites(String shopperId, String customerJwt, UUID vmId) {
        List<VmCdnSite> vmCdnSiteList = cdnDataService.getActiveCdnSitesOfVm(vmId);
        List<CdnSite> cdnSites =  cdnClientService.getCdnSites(getAuthToken(shopperId, customerJwt));

        List<String> vmCdnSiteIds = vmCdnSiteList.stream().map(site -> site.siteId.toLowerCase()).collect(Collectors.toList());
        cdnSites = cdnSites.stream().filter(cdnSite ->
                vmCdnSiteIds.contains(cdnSite.siteId.toLowerCase())).collect(Collectors.toList());
        return cdnSites;
    }

    @Override
    public CdnDetail getCdnSiteDetail(String shopperId, String customerJwt, String siteId, UUID vmId, boolean skipDbCheck) {
        if (!skipDbCheck) {
            VmCdnSite vmCdnSite = cdnDataService.getCdnSiteFromId(vmId, siteId);
            if (vmCdnSite == null) {
                throw new NotFoundException("Could not find site id " + siteId + " belonging to vmId " + vmId);
            }
        }
        return cdnClientService.getCdnSiteDetail(getAuthToken(shopperId, customerJwt), siteId);
    }

    @Override
    public CdnDetail getCdnSiteDetail(String shopperId, String customerJwt, String siteId, UUID vmId) {
        return getCdnSiteDetail(shopperId, customerJwt, siteId, vmId, false);
    }

    @Override
    public CdnClientInvalidateCacheResponse invalidateCdnCache(String shopperId, String customerJwt, String siteId) {
        return cdnClientService.invalidateCdnCache(getAuthToken(shopperId, customerJwt), siteId);
    }

    @Override
    public CdnClientInvalidateStatusResponse getCdnInvalidateCacheStatus(String shopperId, String customerJwt, String siteId, String invalidationId) {
        CdnClientInvalidateStatusResponse res =  cdnClientService.getCdnInvalidateStatus(getAuthToken(shopperId, customerJwt), siteId, invalidationId);

        return res;
    }

    @Override
    public CdnClientCreateResponse createCdn(String shopperId, String customerJwt, String domain, IpAddress ipAddress, String cacheLevel, String bypassWAF) {
        CdnClientCreateRequest req = new CdnClientCreateRequest();
        CdnOrigin cdnOrigin = new CdnOrigin(domain, ipAddress.ipAddress, 0, true);
        req.domain = domain;
        req.cacheLevel = cacheLevel;
        req.planId = CDN_PLAN_ID;
        req.bypassWAF = bypassWAF;
        req.provider = CDN_CLOUDFLARE_PROVIDER;
        req.autoMinify = new String[]{"js"};
        req.imageOptimization = "off";
        req.sslRedirect = "https";
        req.origins = new CdnOrigin[]{cdnOrigin};
        req.verificationMethod = CdnVerificationMethod.TXT.toString();

        return cdnClientService.createCdnSite(getAuthToken(shopperId, customerJwt), req);
    }

    @Override
    public void validateCdn(String shopperId, String customerJwt, String siteId) {
        cdnClientService.requestCdnValidation(getAuthToken(shopperId, customerJwt), siteId);
    }

    @Override
    public void deleteCdnSite(String shopperId, String customerJwt, String siteId) {
        cdnClientService.deleteCdnSite(getAuthToken(shopperId, customerJwt), siteId);
    }

    @Override
    public void updateCdnSite(String shopperId, String customerJwt, String siteId, CdnCacheLevel cacheLevel, CdnBypassWAF bypassWAF) {
        CdnClientUpdateRequest req = new CdnClientUpdateRequest();
        req.bypassWAF = bypassWAF == null ? null : bypassWAF.toString();
        req.cacheLevel = cacheLevel == null ? null : cacheLevel.toString();
        cdnClientService.modifyCdnSite(getAuthToken(shopperId, customerJwt), siteId, req);
    }
}
