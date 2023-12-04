package com.godaddy.vps4.firewall.model;

public class FirewallCloudflareData {
    public String anyCastIP;
    public FirewallValidation[] certificateValidation;
    public FirewallValidation[] domainValidation;

    public String state;

    public FirewallCloudflareData() {

    }

    public FirewallCloudflareData(String anyCastIP, FirewallValidation[] certificateValidation, FirewallValidation[] domainValidation, String state) {
        this.anyCastIP = anyCastIP;
        this.certificateValidation = certificateValidation;
        this.domainValidation = domainValidation;
        this.state = state;
    }
}
