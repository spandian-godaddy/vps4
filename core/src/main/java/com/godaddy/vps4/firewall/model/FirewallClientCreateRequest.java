package com.godaddy.vps4.firewall.model;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class FirewallClientCreateRequest {
    public String domain;
    public String planId;
    public FirewallOrigin[] origins;
    public String cacheLevel;
    public String bypassWAF;
    public String provider;
    public String verificationMethod;
    public String[] subdomains;
    public String[] autoMinify;
    public String imageOptimization;
    public String sslRedirect;

    public FirewallClientCreateRequest() {} // needed for deserialization
}
