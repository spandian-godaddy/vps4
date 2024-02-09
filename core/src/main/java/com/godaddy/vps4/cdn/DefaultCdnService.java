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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.ws.rs.NotFoundException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

public class DefaultCdnService implements CdnService {
    private static final String CDN_PLAN_ID = "VPSWAFCDNBasic";
    private static final String CDN_CLOUDFLARE_PROVIDER = "CLOUDFLARE";
    private static final Logger logger = LoggerFactory.getLogger(DefaultCdnService.class);

    private final CdnClientService cdnClientService;
    private final CdnDataService cdnDataService;

    @Inject
    public DefaultCdnService(CdnClientService cdnClientService,
                             CdnDataService cdnDataService)
    {
        this.cdnClientService = cdnClientService;
        this.cdnDataService = cdnDataService;
    }

    @Override
    public List<CdnSite> getCdnSites(UUID customerId, UUID vmId) {
        List<CdnSite> returnedSites = new ArrayList<>();
        List<VmCdnSite> vmCdnSiteList = cdnDataService.getActiveCdnSitesOfVm(vmId);
        logger.info("customerId {}", customerId);
        List<CdnSite> cdnSites =  cdnClientService.getCdnSites(customerId);
        List<String> vmCdnSiteIds = vmCdnSiteList.stream().map(site -> site.siteId.toLowerCase()).collect(Collectors.toList());
        if (cdnSites != null ) {
            returnedSites = cdnSites.stream().filter(cdnSite ->
                    vmCdnSiteIds.contains(cdnSite.siteId.toLowerCase())).collect(Collectors.toList());
        }
        return returnedSites;
    }

    @Override
    public CdnDetail getCdnSiteDetail(UUID customerId, String siteId, UUID vmId, boolean skipDbCheck) {
        if (!skipDbCheck) {
            VmCdnSite vmCdnSite = cdnDataService.getCdnSiteFromId(vmId, siteId);
            if (vmCdnSite == null) {
                throw new NotFoundException("Could not find site id " + siteId + " belonging to vmId " + vmId);
            }
        }
        return cdnClientService.getCdnSiteDetail(customerId, siteId);
    }

    @Override
    public CdnDetail getCdnSiteDetail(UUID customerId, String siteId, UUID vmId) {
        return getCdnSiteDetail(customerId, siteId, vmId, false);
    }

    @Override
    public CdnClientInvalidateCacheResponse invalidateCdnCache(UUID customerId, String siteId) {
        return cdnClientService.invalidateCdnCache(customerId, siteId);
    }

    @Override
    public CdnClientInvalidateStatusResponse getCdnInvalidateCacheStatus(UUID customerId, String siteId, String invalidationId) {
        CdnClientInvalidateStatusResponse res =  cdnClientService.getCdnInvalidateStatus(customerId, siteId, invalidationId);

        return res;
    }

    @Override
    public CdnClientCreateResponse createCdn(UUID customerId, String domain, IpAddress ipAddress, String cacheLevel, String bypassWAF) {
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

        return cdnClientService.createCdnSite(customerId, req);
    }

    @Override
    public void validateCdn(UUID customerId, String siteId) {
        cdnClientService.requestCdnValidation(customerId, siteId);
    }

    @Override
    public void deleteCdnSite(UUID customerId, String siteId) {
        cdnClientService.deleteCdnSite(customerId, siteId);
    }

    @Override
    public void updateCdnSite(UUID customerId, String siteId, CdnCacheLevel cacheLevel, CdnBypassWAF bypassWAF) {
        CdnClientUpdateRequest req = new CdnClientUpdateRequest();
        req.bypassWAF = bypassWAF == null ? null : bypassWAF.toString();
        req.cacheLevel = cacheLevel == null ? null : cacheLevel.toString();
        cdnClientService.modifyCdnSite(customerId, siteId, req);
    }
}
