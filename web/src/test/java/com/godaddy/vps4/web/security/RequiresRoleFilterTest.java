package com.godaddy.vps4.web.security;

import org.junit.Before;
import org.junit.Test;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.core.Response;
import java.lang.reflect.Field;
import java.util.Arrays;

import com.godaddy.vps4.web.security.GDUser.Role;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;


public class RequiresRoleFilterTest {

    GDUser gdUser;
    ContainerRequestContext requestContext = mock(ContainerRequestContext.class);
    HttpServletRequest httpRequest = mock(HttpServletRequest.class);
    RequiresRoleFilter requestFilter;

    @Before
    public void setupTest() {
        gdUser = new GDUser();
        when(httpRequest.getAttribute(AuthenticationFilter.USER_ATTRIBUTE_NAME)).thenReturn(gdUser);

        requestFilter = new RequiresRoleFilter();
        try {
            Field httpReqField = requestFilter.getClass().getDeclaredField("request");
            httpReqField.setAccessible(true);
            httpReqField.set(requestFilter, httpRequest);
        }
        catch (NoSuchFieldException | IllegalAccessException e) {

        }
    }

    @Test
    public void filterAbortsJaxRSMethodInvocationIfRoleDoesnotMatch() throws Exception {
        gdUser.roles = Arrays.asList(Role.CUSTOMER);
        Role[] roles = {Role.ADMIN, Role.HS_AGENT};
        requestFilter.setReqdRoles(roles);
        requestFilter.filter(requestContext);

        verify(requestContext, times(1)).abortWith(any(Response.class));
    }


    @Test
    public void filterAbortsJaxRSMethodInvocationIfMultipleRolesDonotMatch() throws Exception {
        gdUser.roles = Arrays.asList(Role.CUSTOMER, Role.EMPLOYEE_OTHER, Role.SUSPEND_AUTH);
        Role[] roles = {Role.ADMIN, Role.HS_AGENT};
        requestFilter.setReqdRoles(roles);
        requestFilter.filter(requestContext);

        verify(requestContext, times(1)).abortWith(any(Response.class));
    }

    @Test
    public void filterFallsThroughToJaxRSMethodInvocationIfRoleMatches() throws Exception {
        gdUser.roles = Arrays.asList(Role.ADMIN);
        Role[] roles = {Role.ADMIN, Role.HS_AGENT};
        requestFilter.setReqdRoles(roles);
        requestFilter.filter(requestContext);

        verify(requestContext, times(0)).abortWith(any(Response.class));
    }


    @Test
    public void filterFallsThroughToJaxRSMethodInvocationIfOneRoleMatches() throws Exception {
        gdUser.roles = Arrays.asList(Role.ADMIN, Role.EMPLOYEE_OTHER);
        Role[] roles = {Role.HS_AGENT, Role.ADMIN, Role.C3_OTHER};
        requestFilter.setReqdRoles(roles);
        requestFilter.filter(requestContext);

        verify(requestContext, times(0)).abortWith(any(Response.class));
    }


    @Test
    public void filterFallsThroughToJaxRSMethodInvocationIfMultipleRoleMatches() throws Exception {
        gdUser.roles = Arrays.asList(Role.ADMIN, Role.C3_OTHER, Role.HS_AGENT);
        Role[] roles = {Role.ADMIN, Role.HS_AGENT};
        requestFilter.setReqdRoles(roles);
        requestFilter.filter(requestContext);

        verify(requestContext, times(0)).abortWith(any(Response.class));
    }
}
