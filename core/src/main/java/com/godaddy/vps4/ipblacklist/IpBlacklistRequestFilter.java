package com.godaddy.vps4.ipblacklist;

import javax.inject.Inject;
import javax.ws.rs.client.ClientRequestContext;
import javax.ws.rs.client.ClientRequestFilter;
import javax.ws.rs.core.CacheControl;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;

import com.godaddy.hfs.config.Config;

public class IpBlacklistRequestFilter implements ClientRequestFilter {
    private final Config config;

    @Inject
    public IpBlacklistRequestFilter(Config config) {
        this.config = config;
    }

    @Override
    public void filter(ClientRequestContext clientRequestContext) {
        String creds = config.get("ipblacklist.api.creds");
        if (creds != null) {
            clientRequestContext.getHeaders().add("Authorization", "Basic " + creds);
        }
        clientRequestContext.getHeaders().add(HttpHeaders.CACHE_CONTROL, "no-cache");
        clientRequestContext.getHeaders().add(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON);
    }
}
