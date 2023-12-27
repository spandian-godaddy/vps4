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
    List<CdnSite> getCdnSites(String shopperId, String customerJwt, UUID vmId);
    CdnDetail getCdnSiteDetail(String shopperId, String customerJwt, String siteId, UUID vmId, boolean skipDbCheck);
    CdnDetail getCdnSiteDetail(String shopperId, String customerJwt, String siteId, UUID vmId);
    CdnClientInvalidateCacheResponse invalidateCdnCache(String shopperId, String customerJwt, String siteId);
    CdnClientInvalidateStatusResponse getCdnInvalidateCacheStatus(String shopperId, String customerJwt, String siteId, String invalidationId);
    CdnClientCreateResponse createCdn(String shopperId, String customerJwt, String domain, IpAddress ipAddress, String cacheLevel, String bypassWAF);
    void deleteCdnSite(String shopperId, String customerJwt, String siteId);
    void updateCdnSite(String shopperId, String customerJwt, String siteId, CdnCacheLevel cacheLevel, CdnBypassWAF bypassWAF);
}
