package com.godaddy.vps4.web.security;

import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.container.ResourceInfo;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.Provider;
import java.io.IOException;
import java.lang.reflect.Method;

@Provider
public class TemporarilyDisabledEndpointFilter implements ContainerRequestFilter {

    @Context private ResourceInfo resourceInfo;

    @Override
    public void filter(ContainerRequestContext requestContext) throws IOException {
        Method resourceMethod = resourceInfo.getResourceMethod();
        if (isEndpointTemporarilyDisabled(resourceMethod)) {
            requestContext.abortWith(Response.status(Response.Status.SERVICE_UNAVAILABLE)
                    .entity("Service temporarily unavailable.")
                    .build());
        }
    }

    private boolean isEndpointTemporarilyDisabled(Method resourceMethod) {
        return resourceMethod.isAnnotationPresent(TemporarilyDisabled.class);
    }

}