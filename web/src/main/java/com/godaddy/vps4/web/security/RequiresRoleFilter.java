package com.godaddy.vps4.web.security;

import static com.godaddy.vps4.web.security.Utils.REQUIRES_ROLE_FILTER_PRIORITY;

import javax.annotation.Priority;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import java.io.IOException;
import java.util.Arrays;

import com.godaddy.vps4.web.security.GDUser.Role;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


@Priority(REQUIRES_ROLE_FILTER_PRIORITY)
public class RequiresRoleFilter implements ContainerRequestFilter {

    private static final Logger logger = LoggerFactory.getLogger(RequiresRoleFilter.class);

    @Context
    private HttpServletRequest request;

    Role[] reqdRoles;

    public void setReqdRoles(Role[] reqdRoles) {
        this.reqdRoles = reqdRoles;
    }

    @Override
    public void filter(ContainerRequestContext requestContext) throws IOException {
        GDUser user = (GDUser) request.getAttribute(AuthenticationFilter.USER_ATTRIBUTE_NAME);
        logger.info(String.format("User role: %s, Filter reqdRoles: [%s]", user.roles, Arrays.toString(reqdRoles)));

        if (!user.anyRole(reqdRoles)) {
            requestContext.abortWith(Response.status(Response.Status.FORBIDDEN).build());
        }
    }

}