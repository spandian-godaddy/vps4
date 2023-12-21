package com.godaddy.vps4.web.firewall;

import com.godaddy.vps4.firewall.model.FirewallBypassWAF;
import com.godaddy.vps4.firewall.model.FirewallCacheLevel;

public class VmCreateFirewallRequest {

    public String domain;
    public String ipAddress;
    public FirewallBypassWAF bypassWAF;
    public FirewallCacheLevel cacheLevel;

    // Empty constructor required for Jackson
    public VmCreateFirewallRequest() {}

    public VmCreateFirewallRequest(FirewallBypassWAF bypassWAF, FirewallCacheLevel cacheLevel) {
        this.bypassWAF = bypassWAF;
        this.cacheLevel = cacheLevel;
    }
}
