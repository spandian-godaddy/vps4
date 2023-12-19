package com.godaddy.vps4.web.firewall;

import com.godaddy.vps4.firewall.model.FirewallBypassWAF;
import com.godaddy.vps4.firewall.model.FirewallCacheLevel;

public class VmUpdateFirewallRequest {
    public FirewallBypassWAF bypassWAF;
    public FirewallCacheLevel cacheLevel;

    // Empty constructor required for Jackson
    public VmUpdateFirewallRequest() {}

    public VmUpdateFirewallRequest(FirewallBypassWAF bypassWAF, FirewallCacheLevel cacheLevel) {
        this.bypassWAF = bypassWAF;
        this.cacheLevel = cacheLevel;
    }
}
