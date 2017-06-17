package com.godaddy.vps4.web.security;

import java.io.IOException;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.container.ResourceInfo;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.Provider;

//@Provider
//@ServerInterceptor
//public class AdminAuthInterceptor implements PreProcessInterceptor{
//
//    @Override
//    public ServerResponse preProcess(HttpRequest request, ResourceMethodInvoker method)
//            throws Failure, WebApplicationException {
//        GDUser user = (GDUser) request.getAttribute(AuthenticationFilter.USER_ATTRIBUTE_NAME);
//        if (method.getMethod().isAnnotationPresent(AdminOnly.class) && !user.isStaff())
//            return new ServerResponse("Admin-only resource", 403, new Headers<>());
//        return null;
//    }
//}

@Provider
public class AdminAuthFilter implements ContainerRequestFilter {

    @Context
    private ResourceInfo resourceInfo;

    @Context
    private HttpServletRequest request;

    @Override
    public void filter(ContainerRequestContext requestContext) throws IOException {
        GDUser user = (GDUser) request.getAttribute(SsoAuthenticationFilter.USER_ATTRIBUTE_NAME);
        if (resourceInfo.getResourceMethod().isAnnotationPresent(AdminOnly.class) && !user.isStaff())
            requestContext.abortWith(Response.status(Response.Status.FORBIDDEN).build());
    }
}