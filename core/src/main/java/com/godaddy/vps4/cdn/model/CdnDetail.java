package com.godaddy.vps4.cdn.model;

public class CdnDetail {
    public String siteId;
    public String domain;
    public String planId;
    public CdnStatus status;
    public CdnVerificationMethod verificationMethod;
    public String provider;
    public String cacheLevel;
    public String bypassWAF;
    public String imageOptimization;
    public CdnProductData productData;
    public CdnDetail() {
    }
    public CdnDetail(String siteId, String domain, String planId, CdnStatus status, CdnVerificationMethod verificationMethod,
                     String provider, String cacheLevel, String imageOptimization, CdnProductData productData) {
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
