package com.godaddy.vps4.web.security;

import gdg.hfs.orchestration.web.CommandsResource;
import gdg.hfs.orchestration.web.CommandsViewResource;

import java.io.IOException;
import java.lang.reflect.Method;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.container.ResourceInfo;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.Provider;

@Provider
public class AdminAuthFilter implements ContainerRequestFilter {

    @Context
    private ResourceInfo resourceInfo;

    @Context
    private HttpServletRequest request;

    @Override
    public void filter(ContainerRequestContext requestContext) throws IOException {
        GDUser user = (GDUser) request.getAttribute(AuthenticationFilter.USER_ATTRIBUTE_NAME);
        Method resourceMethod = resourceInfo.getResourceMethod();

        boolean employeeRequired =(resourceInfo.getResourceClass().isAssignableFrom(CommandsResource.class) ||
                                   resourceInfo.getResourceClass().isAssignableFrom(CommandsViewResource.class) ||
                                   resourceMethod.isAnnotationPresent(EmployeeOnly.class));

        boolean adminRequired = resourceMethod.isAnnotationPresent(AdminOnly.class);

        if ((!user.isEmployee && employeeRequired) || (!user.isAdmin() && adminRequired)) {
            requestContext.abortWith(Response.status(Response.Status.FORBIDDEN).build());
        }
    }

}