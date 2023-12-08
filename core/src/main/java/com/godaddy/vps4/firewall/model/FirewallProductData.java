package com.godaddy.vps4.firewall.model;

import com.fasterxml.jackson.annotation.JsonAlias;

public class FirewallProductData {
    @JsonAlias({"cloudFlare"}) public FirewallCloudflareData cloudflare;
    public FirewallProductData() {
    }
    public FirewallProductData(FirewallCloudflareData cloudflare) {
        this.cloudflare = cloudflare;
    }
}
