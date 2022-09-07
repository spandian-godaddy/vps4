package com.godaddy.vps4.jsd;

import com.godaddy.hfs.config.Config;

import javax.inject.Inject;
import javax.ws.rs.client.ClientRequestContext;
import javax.ws.rs.client.ClientRequestFilter;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;

public class JsdClientRequestFilter implements ClientRequestFilter {
    private final Config config;

    @Inject
    public JsdClientRequestFilter(Config config) {
        this.config = config;
    }

    @Override
    public void filter(ClientRequestContext clientRequestContext) {
        String jsdApiKey = config.get("jsd.api.key");
        if (jsdApiKey != null) {
            clientRequestContext.getHeaders().add("Authorization", "Basic " + jsdApiKey);
        }
        clientRequestContext.getHeaders().add(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON);
    }
}
