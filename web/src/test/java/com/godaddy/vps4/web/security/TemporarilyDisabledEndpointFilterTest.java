package com.godaddy.vps4.web.security;

import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Injector;
import org.junit.Before;
import org.junit.Test;

import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.container.ResourceInfo;
import javax.ws.rs.core.Response;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class TemporarilyDisabledEndpointFilterTest {
    private static class TestClass {

        @TemporarilyDisabled
        public void methodOne() {
        }

        public void methodTwo() {
        }
    }

    TestClass testObj;
    ResourceInfo resourceInfo = mock(ResourceInfo.class);
    ContainerRequestContext requestContext = mock(ContainerRequestContext.class);
    ContainerRequestFilter requestFilter;

    private Injector injector = Guice.createInjector(
        new AbstractModule() {
            @Override
            protected void configure() {
                bind(ContainerRequestFilter.class).to(TemporarilyDisabledEndpointFilter.class);
                bind(ResourceInfo.class).toInstance(resourceInfo);
            }
        }
    );

    @Before
    public void setupTest() {
        testObj = new TestClass();
        requestFilter = injector.getInstance(ContainerRequestFilter.class);
        try {
            Field resInfoField = requestFilter.getClass().getDeclaredField("resourceInfo");
            resInfoField.setAccessible(true);
            resInfoField.set(requestFilter, resourceInfo);
        }
        catch (NoSuchFieldException | IllegalAccessException e) {

        }
    }

    @Test
    public void filterAbortsJaxRSMethodInvocationIfMarkedDisabled() throws Exception {
        Method m = TestClass.class.getMethod("methodOne");
        when(resourceInfo.getResourceMethod()).thenReturn(m);
        requestFilter.filter(requestContext);

        verify(requestContext, times(1)).abortWith(any(Response.class));
    }

    @Test
    public void filterFallsThroughToJaxRSMethodInvocationIfNotDisabled() throws Exception {
        Method m = TestClass.class.getMethod("methodTwo");
        when(resourceInfo.getResourceMethod()).thenReturn(m);
        requestFilter.filter(requestContext);

        verify(requestContext, times(0)).abortWith(any(Response.class));
    }

}