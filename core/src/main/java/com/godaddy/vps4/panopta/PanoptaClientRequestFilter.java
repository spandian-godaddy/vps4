package com.godaddy.vps4.panopta;

import javax.inject.Inject;
import javax.ws.rs.client.ClientRequestContext;
import javax.ws.rs.client.ClientRequestFilter;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;

import com.godaddy.hfs.config.Config;

public class PanoptaClientRequestFilter implements ClientRequestFilter {
    private final Config config;

    @Inject
    public PanoptaClientRequestFilter(Config config) {
        this.config = config;
    }

    @Override
    public void filter(ClientRequestContext clientRequestContext) {
        String panoptaApiKey = config.get("panopta.api.key");
        if (panoptaApiKey != null) {
            clientRequestContext.getHeaders().add("Authorization", "ApiKey " + panoptaApiKey);
        }
        clientRequestContext.getHeaders().add(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON);
    }
}
