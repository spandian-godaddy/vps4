package com.godaddy.vps4.firewall.model;

public class FirewallProductData {
    public FirewallCloudflareData cloudflare;
    public FirewallProductData() {
    }
    public FirewallProductData(FirewallCloudflareData cloudflare) {
        this.cloudflare = cloudflare;
    }
}
