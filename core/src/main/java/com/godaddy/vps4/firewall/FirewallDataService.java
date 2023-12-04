package com.godaddy.vps4.firewall;

import com.godaddy.vps4.firewall.model.VmFirewallSite;

import java.util.List;
import java.util.UUID;

public interface FirewallDataService {

    void createFirewallSite(UUID vmId, long ipAddressId, String domain, String siteId);
    VmFirewallSite getFirewallSiteFromId(String siteId);
    List<VmFirewallSite> getActiveFirewallSitesOfVm(UUID vmId);

    void destroyFirewallSite(String siteId);
}