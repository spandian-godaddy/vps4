package com.godaddy.vps4.web.security;

import javax.ws.rs.container.DynamicFeature;
import javax.ws.rs.container.ResourceInfo;
import javax.ws.rs.core.FeatureContext;
import javax.ws.rs.ext.Provider;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.Inject;
import com.google.inject.Injector;

import gdg.hfs.orchestration.web.CommandsResource;
import gdg.hfs.orchestration.web.CommandsViewResource;


@Provider
public class OrchestrationCommandsAuthFeature implements DynamicFeature {

    private static final Logger logger = LoggerFactory.getLogger(OrchestrationCommandsAuthFeature.class);

    Injector injector;

    @Inject
    public OrchestrationCommandsAuthFeature(Injector injector) {
        this.injector = injector;
    }

    @Override
    public void configure(ResourceInfo resourceInfo, FeatureContext featureContext) {
        if (isFeatureNeeded(resourceInfo)) {
            StaffRequiredFilter filter = this.injector.getInstance(StaffRequiredFilter.class);
            logger.info(String.format("Filter %s attached to resource class/method: [%s/%s]",
                filter.getClass().getSimpleName(), resourceInfo.getResourceClass().getSimpleName(),
                resourceInfo.getResourceMethod().getName()));

            featureContext.register(filter);
        }
    }

    private boolean isFeatureNeeded(ResourceInfo resourceInfo) {
        return (resourceInfo.getResourceClass().isAssignableFrom(CommandsResource.class) ||
                resourceInfo.getResourceClass().isAssignableFrom(CommandsViewResource.class));
    }
}