package com.godaddy.vps4.web.security;

import com.google.inject.Inject;
import com.google.inject.Injector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.container.DynamicFeature;
import javax.ws.rs.container.ResourceInfo;
import javax.ws.rs.core.FeatureContext;
import javax.ws.rs.ext.Provider;
import java.lang.reflect.Method;

@Provider
public class TemporarilyDisabledFeature implements DynamicFeature {

    private static final Logger logger = LoggerFactory.getLogger(TemporarilyDisabledFeature.class);

    Injector injector;

    @Inject
    public TemporarilyDisabledFeature(Injector injector) {
        this.injector = injector;
    }

    @Override
    public void configure(ResourceInfo resourceInfo, FeatureContext featureContext) {
        Method resourceMethod = resourceInfo.getResourceMethod();

        if (isEndpointTemporarilyDisabled(resourceMethod)) {
            TemporarilyDisabledEndpointFilter filter
                = this.injector.getInstance(TemporarilyDisabledEndpointFilter.class);
            logger.info(String.format("Filter %s attached to resource class/method: [%s/%s]",
                    filter.getClass().getSimpleName(), resourceInfo.getResourceClass().getSimpleName(),
                    resourceInfo.getResourceMethod().getName()));

            featureContext.register(filter);
        }
    }

    private boolean isEndpointTemporarilyDisabled(Method resourceMethod) {
        return resourceMethod.isAnnotationPresent(TemporarilyDisabled.class);
    }
}