package com.godaddy.vps4.firewall;

import com.godaddy.vps4.firewall.model.FirewallBypassWAF;
import com.godaddy.vps4.firewall.model.FirewallCacheLevel;
import com.godaddy.vps4.firewall.model.FirewallClientCreateResponse;
import com.godaddy.vps4.firewall.model.FirewallClientInvalidateCacheResponse;
import com.godaddy.vps4.firewall.model.FirewallClientInvalidateStatusResponse;
import com.godaddy.vps4.firewall.model.FirewallDetail;
import com.godaddy.vps4.firewall.model.FirewallSite;
import com.godaddy.vps4.network.IpAddress;

import java.util.List;
import java.util.UUID;

public interface FirewallService {
    List<FirewallSite> getFirewallSites(String shopperId, String customerJwt, UUID vmId);
    FirewallDetail getFirewallSiteDetail(String shopperId, String customerJwt, String siteId, UUID vmId, boolean skipDbCheck);
    FirewallDetail getFirewallSiteDetail(String shopperId, String customerJwt, String siteId, UUID vmId);
    FirewallClientInvalidateCacheResponse invalidateFirewallCache(String shopperId, String customerJwt, String siteId);
    FirewallClientInvalidateStatusResponse getFirewallInvalidateCacheStatus(String shopperId, String customerJwt, String siteId, String invalidationId);
    FirewallClientCreateResponse createFirewall(String shopperId, String customerJwt, String domain, IpAddress ipAddress, String cacheLevel, String bypassWAF);
    void deleteFirewallSite(String shopperId, String customerJwt, String siteId);
    void updateFirewallSite(String shopperId, String customerJwt, String siteId, FirewallCacheLevel cacheLevel, FirewallBypassWAF bypassWAF);
}
