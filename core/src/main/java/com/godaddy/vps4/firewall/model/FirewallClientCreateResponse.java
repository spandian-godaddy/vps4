package com.godaddy.vps4.firewall.model;

public class FirewallClientCreateResponse {
    public String siteId;
    public int revision;

    public FirewallClientCreateResponse(String siteId, int revision) {
        this.siteId = siteId;
        this.revision = revision;
    }
}
