package com.godaddy.vps4.firewall;

import com.godaddy.vps4.firewall.model.FirewallDetail;
import com.godaddy.vps4.firewall.model.FirewallSite;

public interface FirewallService {
    FirewallSite[] getAllFirewallSites(String shopperId, String customerJwt);


    FirewallDetail getFirewallSiteDetail(String shopperId, String customerJwt, String siteId);
}
