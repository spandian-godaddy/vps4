package com.godaddy.vps4.web.security;

import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Injector;
import org.junit.Before;
import org.junit.Test;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.container.ResourceInfo;
import javax.ws.rs.core.Response;
import java.lang.reflect.Field;
import java.lang.reflect.Method;

import com.godaddy.vps4.web.security.GDUser.Role;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class RequiresRoleFilterTest {
    private static class TestClass {

        @RequiresRole(roles = {Role.ADMIN, Role.HS_AGENT, Role.HS_LEAD})
        public void methodOne() {
        }

        public void methodTwo() {
        }
    }

    GDUser gdUser;
    ResourceInfo resourceInfo = mock(ResourceInfo.class);
    ContainerRequestContext requestContext = mock(ContainerRequestContext.class);
    HttpServletRequest httpRequest = mock(HttpServletRequest.class);
    ContainerRequestFilter requestFilter;

    private Injector injector = Guice.createInjector(
        new AbstractModule() {
            @Override
            protected void configure() {
                bind(ContainerRequestFilter.class).to(RequiresRoleFilter.class);
            }
        }
    );

    @Before
    public void setupTest() {
        gdUser = new GDUser();
        when(httpRequest.getAttribute(AuthenticationFilter.USER_ATTRIBUTE_NAME)).thenReturn(gdUser);

        requestFilter = injector.getInstance(ContainerRequestFilter.class);
        try {
            Field resInfoField = requestFilter.getClass().getDeclaredField("resourceInfo");
            resInfoField.setAccessible(true);
            resInfoField.set(requestFilter, resourceInfo);

            Field httpReqField = requestFilter.getClass().getDeclaredField("request");
            httpReqField.setAccessible(true);
            httpReqField.set(requestFilter, httpRequest);
        }
        catch (NoSuchFieldException | IllegalAccessException e) {

        }
    }

    @Test
    public void filterAbortsJaxRSMethodInvocationIfRoleDoesnotMatch() throws Exception {
        gdUser.role = Role.CUSTOMER;
        Method m = TestClass.class.getMethod("methodOne");
        when(resourceInfo.getResourceMethod()).thenReturn(m);
        requestFilter.filter(requestContext);

        verify(requestContext, times(1)).abortWith(any(Response.class));
    }

    @Test
    public void filterFallsThroughToJaxRSMethodInvocationIfRoleMatches() throws Exception {
        gdUser.role = Role.ADMIN;
        Method m = TestClass.class.getMethod("methodOne");
        when(resourceInfo.getResourceMethod()).thenReturn(m);
        requestFilter.filter(requestContext);

        verify(requestContext, times(0)).abortWith(any(Response.class));
    }

    @Test
    public void filterFallsThroughToJaxRSMethodInvocationIfAnnotationNotPresent() throws Exception {
        Method m = TestClass.class.getMethod("methodTwo");
        when(resourceInfo.getResourceMethod()).thenReturn(m);
        requestFilter.filter(requestContext);

        verify(requestContext, times(0)).abortWith(any(Response.class));
    }

}