package com.godaddy.vps4.cdn.model;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class CdnClientCreateRequest {
    public String domain;
    public String planId;
    public CdnOrigin[] origins;
    public String cacheLevel;
    public String bypassWAF;
    public String provider;
    public String verificationMethod;
    public String[] subdomains;
    public String[] autoMinify;
    public String imageOptimization;
    public String sslRedirect;

    public CdnClientCreateRequest() {} // needed for deserialization
}
