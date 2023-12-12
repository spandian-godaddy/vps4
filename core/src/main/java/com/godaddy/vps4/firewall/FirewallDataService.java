package com.godaddy.vps4.firewall;

import com.godaddy.vps4.firewall.model.VmFirewallSite;

import java.util.List;
import java.util.UUID;

public interface FirewallDataService {

    void createFirewallSite(UUID vmId, long ipAddressId, String domain, String siteId);
    VmFirewallSite getFirewallSiteFromId(UUID vmId, String siteId);
    List<VmFirewallSite> getActiveFirewallSitesOfVm(UUID vmId);

    void destroyFirewallSite(UUID vmId, String siteId);
}