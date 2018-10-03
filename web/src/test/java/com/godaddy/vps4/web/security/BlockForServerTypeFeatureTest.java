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
import org.junit.Before;
import org.junit.Test;
import com.godaddy.vps4.vm.ServerType.Type;

import javax.ws.rs.container.DynamicFeature;
import javax.ws.rs.container.ResourceInfo;
import javax.ws.rs.core.FeatureContext;
import java.lang.reflect.Method;


public class BlockForServerTypeFeatureTest {
    private static class TestClassOne {

        @BlockServerType(serverTypes = {Type.DEDICATED})
        public void methodOne() {
        }

        public void methodTwo() {
        }
    }

    @BlockServerType(serverTypes = {Type.VIRTUAL})
    private static class TestClassTwo {

        public void methodOne() {
        }
    }

    private ResourceInfo resourceInfo;
    private FeatureContext featureContext;
    private DynamicFeature feature;
    private BlockForServerTypeFilter blockForServerTypeFilter;
    private Injector injector;

    @Before
    public void setUp() throws Exception {
        featureContext = mock(FeatureContext.class);
        resourceInfo = mock(ResourceInfo.class);
        blockForServerTypeFilter = mock(BlockForServerTypeFilter.class);

        injector = Guice.createInjector(
            new AbstractModule() {
                @Override
                protected void configure() {
                    bind(DynamicFeature.class).to(BlockForServerTypeFeature.class);
                    bind(BlockForServerTypeFilter.class).toInstance(blockForServerTypeFilter);
                }
            }
        );

        feature = injector.getInstance(DynamicFeature.class);
    }

    @Test
    public void filterNotAttachedToResourceMethodIfNotReqd() throws NoSuchMethodException{
        Method m = TestClassOne.class.getMethod("methodTwo");
        when(resourceInfo.getResourceMethod()).thenReturn(m);
        when(resourceInfo.getResourceClass()).thenReturn((Class) TestClassOne.class);
        feature.configure(resourceInfo, featureContext);

        verify(featureContext, times(0)).register(any());
    }

    @Test
    public void filterAttachedToResourceMethodIfAnnotationPresentOnMethod() throws NoSuchMethodException{
        Method m = TestClassOne.class.getMethod("methodOne");
        when(resourceInfo.getResourceMethod()).thenReturn(m);
        when(resourceInfo.getResourceClass()).thenReturn((Class) TestClassOne.class);
        feature.configure(resourceInfo, featureContext);

        verify(featureContext, times(1)).register(eq(blockForServerTypeFilter));
    }

    @Test
    public void filterAttachedToResourceMethodIfAnnotationPresentOnClass() throws NoSuchMethodException{
        Method m = TestClassTwo.class.getMethod("methodOne");
        when(resourceInfo.getResourceMethod()).thenReturn(m);
        when(resourceInfo.getResourceClass()).thenReturn((Class) TestClassTwo.class);
        feature.configure(resourceInfo, featureContext);

        verify(featureContext, times(1)).register(eq(blockForServerTypeFilter));

        Type[] serverTypes = TestClassTwo.class.getAnnotation(BlockServerType.class).serverTypes();
        verify(blockForServerTypeFilter, times(1)).setServerTypesToBlock(eq(serverTypes));
    }

    @Test
    public void filterIsAssignedServerTypesToBlock() throws NoSuchMethodException{
        Method m = TestClassOne.class.getMethod("methodOne");
        when(resourceInfo.getResourceMethod()).thenReturn(m);
        when(resourceInfo.getResourceClass()).thenReturn((Class) TestClassOne.class);
        feature.configure(resourceInfo, featureContext);

        Type[] serverTypes = m.getAnnotation(BlockServerType.class).serverTypes();
        verify(blockForServerTypeFilter, times(1)).setServerTypesToBlock(eq(serverTypes));
    }
}