package com.godaddy.vps4.web.security;

import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.core.Response;
import java.io.IOException;


public class TemporarilyDisabledEndpointFilter implements ContainerRequestFilter {

    @Override
    public void filter(ContainerRequestContext requestContext) throws IOException {
        requestContext.abortWith(Response.status(Response.Status.SERVICE_UNAVAILABLE)
            .entity("Service temporarily unavailable.")
            .build());
    }
}
