package com.godaddy.vps4.firewall.model;

public class FirewallDetail {
    public String siteId;
    public String domain;
    public String planId;
    public String status;
    public FirewallVerificationMethod verificationMethod;
    public String provider;
    public String cacheLevel;
    public String imageOptimization;
    public FirewallProductData productData;
    public FirewallDetail() {
    }
    public FirewallDetail(String siteId, String domain, String planId, String status, FirewallVerificationMethod verificationMethod,
                          String provider, String cacheLevel, String imageOptimization, FirewallProductData productData) {
        this.siteId = siteId;
        this.domain = domain;
        this.planId = planId;
        this.status = status;
        this.verificationMethod = verificationMethod;
        this.provider = provider;
        this.cacheLevel = cacheLevel;
        this.imageOptimization = imageOptimization;
        this.productData = productData;
    }
}
