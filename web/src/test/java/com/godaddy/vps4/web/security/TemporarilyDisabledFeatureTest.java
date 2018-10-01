package com.godaddy.vps4.web.security;

import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Injector;
import org.junit.Before;
import org.junit.Test;

import javax.ws.rs.container.DynamicFeature;
import javax.ws.rs.container.ResourceInfo;
import javax.ws.rs.core.FeatureContext;
import java.lang.reflect.Method;

import static org.mockito.Mockito.any;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;


public class TemporarilyDisabledFeatureTest {
    private static class TestClass {

        @TemporarilyDisabled
        public void methodOne() {
        }

        public void methodTwo() {
        }
    }

    private ResourceInfo resourceInfo;
    private FeatureContext featureContext;
    private DynamicFeature feature;
    private TemporarilyDisabledEndpointFilter temporarilyDisabledEndpointFilter;
    private Injector injector;

    @Before
    public void setUp() throws Exception {
        featureContext = mock(FeatureContext.class);
        resourceInfo = mock(ResourceInfo.class);
        temporarilyDisabledEndpointFilter = mock(TemporarilyDisabledEndpointFilter.class);

        injector = Guice.createInjector(
            new AbstractModule() {
                @Override
                protected void configure() {
                    bind(DynamicFeature.class).to(TemporarilyDisabledFeature.class);
                    bind(TemporarilyDisabledEndpointFilter.class).toInstance(temporarilyDisabledEndpointFilter);
                }
            }
        );

        feature = injector.getInstance(DynamicFeature.class);
    }

    @Test
    public void filterNotAttachedToResourceMethodIfNotReqd() throws NoSuchMethodException{
        Method m = TestClass.class.getMethod("methodTwo");
        when(resourceInfo.getResourceMethod()).thenReturn(m);
        when(resourceInfo.getResourceClass()).thenReturn((Class) TestClass.class);
        feature.configure(resourceInfo, featureContext);

        verify(featureContext, times(0)).register(any());
    }

    @Test
    public void filterAttachedToResourceMethodIfAnnotationPresent() throws NoSuchMethodException{
        Method m = TestClass.class.getMethod("methodOne");
        when(resourceInfo.getResourceMethod()).thenReturn(m);
        when(resourceInfo.getResourceClass()).thenReturn((Class) TestClass.class);
        feature.configure(resourceInfo, featureContext);

        verify(featureContext, times(1)).register(eq(temporarilyDisabledEndpointFilter));
    }
}