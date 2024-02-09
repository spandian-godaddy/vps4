package com.godaddy.vps4.cdn;

import com.godaddy.vps4.cdn.model.CdnBypassWAF;
import com.godaddy.vps4.cdn.model.CdnCacheLevel;
import com.godaddy.vps4.cdn.model.CdnClientCreateResponse;
import com.godaddy.vps4.cdn.model.CdnClientInvalidateCacheResponse;
import com.godaddy.vps4.cdn.model.CdnClientInvalidateStatusResponse;
import com.godaddy.vps4.cdn.model.CdnDetail;
import com.godaddy.vps4.cdn.model.CdnSite;
import com.godaddy.vps4.network.IpAddress;

import java.util.List;
import java.util.UUID;

public interface CdnService {
    List<CdnSite> getCdnSites(UUID customerId, UUID vmId);
    CdnDetail getCdnSiteDetail(UUID customerId, String siteId, UUID vmId, boolean skipDbCheck);
    CdnDetail getCdnSiteDetail(UUID customerId, String siteId, UUID vmId);
    CdnClientInvalidateCacheResponse invalidateCdnCache(UUID customerId, String siteId);
    CdnClientInvalidateStatusResponse getCdnInvalidateCacheStatus(UUID customerId, String siteId, String invalidationId);
    CdnClientCreateResponse createCdn(UUID customerId, String domain, IpAddress ipAddress, String cacheLevel, String bypassWAF);

    void validateCdn(UUID customerId, String siteId);

    void deleteCdnSite(UUID customerId, String siteId);
    void updateCdnSite(UUID customerId, String siteId, CdnCacheLevel cacheLevel, CdnBypassWAF bypassWAF);
}
