package com.godaddy.vps4.web.security;

import static com.godaddy.vps4.web.security.Utils.ADMIN_AUTH_FILTER_PRIORITY;

import java.io.IOException;

import javax.annotation.Priority;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.container.ResourceInfo;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;

@Priority(ADMIN_AUTH_FILTER_PRIORITY)
public class StaffRequiredFilter implements ContainerRequestFilter {

    @Context
    private ResourceInfo resourceInfo;

    @Context
    private HttpServletRequest request;

    @Override
    public void filter(ContainerRequestContext requestContext) throws IOException {
        GDUser user = (GDUser) request.getAttribute(AuthenticationFilter.USER_ATTRIBUTE_NAME);

        GDUser.Role[] staffRoles = {GDUser.Role.ADMIN, GDUser.Role.HS_LEAD, GDUser.Role.HS_AGENT, GDUser.Role.SUSPEND_AUTH};
        if (!user.anyRole(staffRoles)) {
            requestContext.abortWith(Response.status(Response.Status.FORBIDDEN).build());
        }
    }

}