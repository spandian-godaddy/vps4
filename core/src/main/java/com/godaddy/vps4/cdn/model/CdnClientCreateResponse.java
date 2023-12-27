package com.godaddy.vps4.cdn.model;

public class CdnClientCreateResponse {
    public String siteId;
    public int revision;

    public CdnClientCreateResponse(String siteId, int revision) {
        this.siteId = siteId;
        this.revision = revision;
    }

    public CdnClientCreateResponse() {} // needed for deserialization
}
