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
            logger.info(String.format(
                "TemporarilyDisabled filter attached to resource/method: [%s/%s]",
                resourceInfo.getResourceClass(), resourceMethod.getName()));

            TemporarilyDisabledEndpointFilter filter
                = this.injector.getInstance(TemporarilyDisabledEndpointFilter.class);
            featureContext.register(filter);
        }
    }

    private boolean isEndpointTemporarilyDisabled(Method resourceMethod) {
        return resourceMethod.isAnnotationPresent(TemporarilyDisabled.class);
    }
}