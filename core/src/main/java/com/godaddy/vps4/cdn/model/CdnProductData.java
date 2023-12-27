package com.godaddy.vps4.cdn.model;

import com.fasterxml.jackson.annotation.JsonAlias;

public class CdnProductData {
    @JsonAlias({"cloudFlare"}) public CdnCloudflareData cloudflare;
    public CdnProductData() {
    }
    public CdnProductData(CdnCloudflareData cloudflare) {
        this.cloudflare = cloudflare;
    }
}
