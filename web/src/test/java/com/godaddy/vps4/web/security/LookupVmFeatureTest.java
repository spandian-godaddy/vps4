package com.godaddy.vps4.web.security;

import com.godaddy.vps4.web.credit.CreditResource;
import com.godaddy.vps4.web.vm.VmResource;
import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Injector;
import org.junit.Before;
import org.junit.Test;

import javax.ws.rs.container.DynamicFeature;
import javax.ws.rs.container.ResourceInfo;
import javax.ws.rs.core.FeatureContext;
import java.lang.reflect.Method;
import java.util.UUID;

import static org.mockito.Mockito.any;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;


public class LookupVmFeatureTest {
    private ResourceInfo resourceInfo;
    private FeatureContext featureContext;
    private DynamicFeature feature;
    private LookupVmFilter lookupVmFilter;
    private Injector injector;

    @Before
    public void setUp() throws Exception {
        featureContext = mock(FeatureContext.class);
        resourceInfo = mock(ResourceInfo.class);
        lookupVmFilter = mock(LookupVmFilter.class);

        injector = Guice.createInjector(
            new AbstractModule() {
                @Override
                protected void configure() {
                    bind(DynamicFeature.class).to(LookupVmFeature.class);
                    bind(LookupVmFilter.class).toInstance(lookupVmFilter);
                }
            }
        );

        feature = injector.getInstance(DynamicFeature.class);
    }

    @Test
    public void filterNotAttachedToResourceMethodIfNotReqd() throws NoSuchMethodException{
        // THE RESOURCE METHOD IS A NOT DIRECTED AT A PARTICULAR VM
        Method m = VmResource.class.getMethod("provisionVm", VmResource.ProvisionVmRequest.class);
        when(resourceInfo.getResourceMethod()).thenReturn(m);
        when(resourceInfo.getResourceClass()).thenReturn((Class) VmResource.class);
        feature.configure(resourceInfo, featureContext);

        verify(featureContext, times(0)).register(any());
    }

    @Test
    public void filterNotAttachedToResourceMethodIfNotReqdTypeTwo() throws NoSuchMethodException{
        // THE RESOURCE CLASS IS NOT RELATED TO VMs
        Method m = CreditResource.class.getMethod("getCredit", UUID.class);
        when(resourceInfo.getResourceMethod()).thenReturn(m);
        when(resourceInfo.getResourceClass()).thenReturn((Class) CreditResource.class);
        feature.configure(resourceInfo, featureContext);

        verify(featureContext, times(0)).register(any());
    }

    @Test
    public void filterAttachedToResourceMethodIfAnnotationPresent() throws NoSuchMethodException{
        Method m = VmResource.class.getMethod("getVm", UUID.class);
        when(resourceInfo.getResourceMethod()).thenReturn(m);
        when(resourceInfo.getResourceClass()).thenReturn((Class) VmResource.class);
        feature.configure(resourceInfo, featureContext);

        verify(featureContext, times(1)).register(eq(lookupVmFilter));
    }
}