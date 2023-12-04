package com.godaddy.vps4.firewall.model;

public class FirewallClientCreateRequest {
    public String domain;
    public String planId;
    public FirewallOrigin[] origins;
    public String cacheLevel;
    public String provider;
    public FirewallVerificationMethod verificationMethod;
    public String[] subdomains;
    public String[] autoMinify;
    public String imageOptimization;
    public String sslRedirect;

}
