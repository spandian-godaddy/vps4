package com.godaddy.vps4.oh;

import java.io.IOException;

import javax.ws.rs.client.ClientRequestContext;
import javax.ws.rs.client.ClientRequestFilter;

public class OhServiceRequestFilter implements ClientRequestFilter {
    private final String auth;

    public OhServiceRequestFilter(String auth) {
        this.auth = auth;
    }

    @Override
    public void filter(ClientRequestContext requestContext) throws IOException {
        requestContext.getHeaders().add("X-Api-Key", auth);
    }
}
