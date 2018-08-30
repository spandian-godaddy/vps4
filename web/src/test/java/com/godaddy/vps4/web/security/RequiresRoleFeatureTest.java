package com.godaddy.vps4.web.security;

import static org.mockito.Mockito.any;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.godaddy.vps4.web.security.GDUser.Role;
import org.junit.Before;
import org.junit.Test;

import javax.ws.rs.container.DynamicFeature;
import javax.ws.rs.container.ResourceInfo;
import javax.ws.rs.core.FeatureContext;
import java.lang.reflect.Method;


public class RequiresRoleFeatureTest {
    private static class TestClass {

        @RequiresRole(roles = {Role.ADMIN, Role.HS_AGENT, Role.HS_LEAD})
        public void methodOne() {
        }

        public void methodTwo() {
        }
    }

    private ResourceInfo resourceInfo;
    private FeatureContext featureContext;
    private DynamicFeature feature;
    private RequiresRoleFilter requiresRoleFilter;
    private Injector injector;

    @Before
    public void setUp() throws Exception {
        featureContext = mock(FeatureContext.class);
        resourceInfo = mock(ResourceInfo.class);
        requiresRoleFilter = mock(RequiresRoleFilter.class);

        injector = Guice.createInjector(
            new AbstractModule() {
                @Override
                protected void configure() {
                    bind(DynamicFeature.class).to(RequiresRoleFeature.class);
                    bind(RequiresRoleFilter.class).toInstance(requiresRoleFilter);
                }
            }
        );

        feature = injector.getInstance(DynamicFeature.class);
    }

    @Test
    public void filterNotAttachedToResourceMethodIfNotReqd() throws NoSuchMethodException{
        Method m = TestClass.class.getMethod("methodTwo");
        when(resourceInfo.getResourceMethod()).thenReturn(m);
        feature.configure(resourceInfo, featureContext);

        verify(featureContext, times(0)).register(any());
    }

    @Test
    public void filterAttachedToResourceMethodIfAnnotationPresent() throws NoSuchMethodException{
        Method m = TestClass.class.getMethod("methodOne");
        when(resourceInfo.getResourceMethod()).thenReturn(m);
        feature.configure(resourceInfo, featureContext);

        verify(featureContext, times(1)).register(eq(requiresRoleFilter));
    }

    @Test
    public void filterIsAssignedRolesToAllow() throws NoSuchMethodException{
        Method m = TestClass.class.getMethod("methodOne");
        when(resourceInfo.getResourceMethod()).thenReturn(m);
        feature.configure(resourceInfo, featureContext);

        Role[] expectedRoles = m.getAnnotation(RequiresRole.class).roles();
        verify(requiresRoleFilter, times(1)).setReqdRoles(eq(expectedRoles));
    }

}