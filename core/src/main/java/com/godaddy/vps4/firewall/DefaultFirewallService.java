package com.godaddy.vps4.firewall;

import com.godaddy.vps4.firewall.model.FirewallClientCreateRequest;
import com.godaddy.vps4.firewall.model.FirewallClientCreateResponse;
import com.godaddy.vps4.firewall.model.FirewallClientInvalidateCacheResponse;
import com.godaddy.vps4.firewall.model.FirewallClientInvalidateStatusResponse;
import com.godaddy.vps4.firewall.model.FirewallClientUpdateRequest;
import com.godaddy.vps4.firewall.model.FirewallDetail;
import com.godaddy.vps4.firewall.model.FirewallOrigin;
import com.godaddy.vps4.firewall.model.FirewallSite;
import com.godaddy.vps4.firewall.model.VmFirewallSite;
import com.godaddy.vps4.firewall.model.FirewallBypassWAF;
import com.godaddy.vps4.firewall.model.FirewallCacheLevel;
import com.godaddy.vps4.firewall.model.FirewallVerificationMethod;
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

public class DefaultFirewallService implements FirewallService {
    private static final String CDN_PLAN_ID = "WSSWAFBasic";
    private static final String CDN_CLOUDFLARE_PROVIDER = "CLOUDFLARE";
    private static final Logger logger = LoggerFactory.getLogger(DefaultFirewallService.class);

    private final Vps4SsoService ssoService;
    private final FirewallClientService firewallClientService;
    private final FirewallDataService firewallDataService;

    @Inject
    public DefaultFirewallService(FirewallClientService firewallClientService,
                                  FirewallDataService firewallDataService,
                                  Vps4SsoService ssoService)
    {
        this.firewallClientService = firewallClientService;
        this.firewallDataService = firewallDataService;
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
    public List<FirewallSite> getFirewallSites(String shopperId, String customerJwt, UUID vmId) {
        List<VmFirewallSite> vmFirewallSiteList = firewallDataService.getActiveFirewallSitesOfVm(vmId);
        List<FirewallSite> firewallSites =  firewallClientService.getFirewallSites(getAuthToken(shopperId, customerJwt));

        List<String> vmFirewallSiteIds = vmFirewallSiteList.stream().map(site -> site.siteId.toLowerCase()).collect(Collectors.toList());
        firewallSites = firewallSites.stream().filter(firewallSite ->
                vmFirewallSiteIds.contains(firewallSite.siteId.toLowerCase())).collect(Collectors.toList());
        return firewallSites;
    }

    @Override
    public FirewallDetail getFirewallSiteDetail(String shopperId, String customerJwt, String siteId, UUID vmId, boolean skipDbCheck) {
        if (!skipDbCheck) {
            VmFirewallSite vmFirewallSite = firewallDataService.getFirewallSiteFromId(vmId, siteId);
            if (vmFirewallSite == null) {
                throw new NotFoundException("Could not find site id " + siteId + " belonging to vmId " + vmId);
            }
        }
        return firewallClientService.getFirewallSiteDetail(getAuthToken(shopperId, customerJwt), siteId);
    }

    @Override
    public FirewallDetail getFirewallSiteDetail(String shopperId, String customerJwt, String siteId, UUID vmId) {
        return getFirewallSiteDetail(shopperId, customerJwt, siteId, vmId, false);
    }

    @Override
    public FirewallClientInvalidateCacheResponse invalidateFirewallCache(String shopperId, String customerJwt, String siteId) {
        return firewallClientService.invalidateFirewallCache(getAuthToken(shopperId, customerJwt), siteId);
    }

    @Override
    public FirewallClientInvalidateStatusResponse getFirewallInvalidateCacheStatus(String shopperId, String customerJwt, String siteId, String invalidationId) {
        return firewallClientService.getFirewallInvalidateStatus(getAuthToken(shopperId, customerJwt), siteId, invalidationId);
    }

    @Override
    public FirewallClientCreateResponse createFirewall(String shopperId, String customerJwt, String domain, IpAddress ipAddress, String cacheLevel, String bypassWAF) {
        FirewallClientCreateRequest req = new FirewallClientCreateRequest();
        FirewallOrigin firewallOrigin = new FirewallOrigin(domain, ipAddress.ipAddress, 0, true);
        req.domain = domain;
        req.cacheLevel = cacheLevel;
        req.planId = CDN_PLAN_ID;
        req.bypassWAF = bypassWAF;
        req.provider = CDN_CLOUDFLARE_PROVIDER;
        req.autoMinify = new String[]{"js"};
        req.imageOptimization = "off";
        req.sslRedirect = "https";
        req.origins = new FirewallOrigin[]{firewallOrigin};
        req.verificationMethod = FirewallVerificationMethod.TXT.toString();

        return firewallClientService.createFirewallSite(getAuthToken(shopperId, customerJwt), req);
    }

    @Override
    public void deleteFirewallSite(String shopperId, String customerJwt, String siteId) {
        firewallClientService.deleteFirewallSite(getAuthToken(shopperId, customerJwt), siteId);
    }

    @Override
    public void updateFirewallSite(String shopperId, String customerJwt, String siteId, FirewallCacheLevel cacheLevel, FirewallBypassWAF bypassWAF) {
        FirewallClientUpdateRequest req = new FirewallClientUpdateRequest();
        req.bypassWAF = bypassWAF == null ? null : bypassWAF.toString();
        req.cacheLevel = cacheLevel == null ? null : cacheLevel.toString();
        firewallClientService.modifyFirewallSite(getAuthToken(shopperId, customerJwt), siteId, req);
    }
}
