package com.godaddy.vps4.web.security;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.container.ResourceInfo;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.Provider;
import java.io.IOException;
import java.lang.reflect.Method;

@Provider
public class RequiresRoleFilter implements ContainerRequestFilter {

    @Context
    private ResourceInfo resourceInfo;

    @Context
    private HttpServletRequest request;

    @Override
    public void filter(ContainerRequestContext requestContext) throws IOException {
        GDUser user = (GDUser) request.getAttribute(AuthenticationFilter.USER_ATTRIBUTE_NAME);
        Method resourceMethod = resourceInfo.getResourceMethod();

        boolean usesRoleBasedAccess = resourceMethod.isAnnotationPresent(RequiresRole.class);

        if (usesRoleBasedAccess && !user.anyRole(resourceMethod.getAnnotation(RequiresRole.class).roles())) {
            requestContext.abortWith(Response.status(Response.Status.FORBIDDEN).build());
        }
    }

}