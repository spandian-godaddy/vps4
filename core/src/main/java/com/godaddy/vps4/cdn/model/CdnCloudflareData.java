package com.godaddy.vps4.cdn.model;

public class CdnCloudflareData {
    public String anyCastIP;
    public CdnValidation[] certificateValidation;
    public CdnValidation[] domainValidation;

    public String state;

    public CdnCloudflareData() {

    }

    public CdnCloudflareData(String anyCastIP, CdnValidation[] certificateValidation, CdnValidation[] domainValidation, String state) {
        this.anyCastIP = anyCastIP;
        this.certificateValidation = certificateValidation;
        this.domainValidation = domainValidation;
        this.state = state;
    }
}
