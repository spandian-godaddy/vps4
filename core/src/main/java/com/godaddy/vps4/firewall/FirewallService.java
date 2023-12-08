package com.godaddy.vps4.firewall;

import com.godaddy.vps4.firewall.model.FirewallDetail;
import com.godaddy.vps4.firewall.model.FirewallSite;
import com.godaddy.vps4.firewall.model.VmFirewallSite;

import java.util.List;
import java.util.UUID;

public interface FirewallService {
    List<FirewallSite> getFirewallSites(String shopperId, String customerJwt, UUID vmId);
    FirewallDetail getFirewallSiteDetail(String shopperId, String customerJwt, String siteId, UUID vmId);
}
