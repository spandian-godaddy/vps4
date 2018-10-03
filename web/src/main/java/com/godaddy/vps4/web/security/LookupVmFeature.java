package com.godaddy.vps4.web.security;

import static com.godaddy.vps4.web.security.Utils.isAVmResourceMethod;

import com.google.inject.Inject;
import com.google.inject.Injector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.container.DynamicFeature;
import javax.ws.rs.container.ResourceInfo;
import javax.ws.rs.core.FeatureContext;
import javax.ws.rs.ext.Provider;


@Provider
public class LookupVmFeature implements DynamicFeature {

    private static final Logger logger = LoggerFactory.getLogger(LookupVmFeature.class);

    Injector injector;

    @Inject
    public LookupVmFeature(Injector injector) {
        this.injector = injector;
    }

    @Override
    public void configure(ResourceInfo resourceInfo, FeatureContext featureContext) {
        if (isVmLookupFilterNeeded(resourceInfo)) {
            LookupVmFilter filter = this.injector.getInstance(LookupVmFilter.class);
            logger.info(String.format("Filter %s attached to resource class/method: [%s/%s]",
                filter.getClass().getSimpleName(), resourceInfo.getResourceClass().getSimpleName(),
                resourceInfo.getResourceMethod().getName()));

            featureContext.register(filter);
        }
    }

    // Lookup vm only if this request is directed at a particular vm
    private boolean isVmLookupFilterNeeded(ResourceInfo resourceInfo) {
        return isAVmResourceMethod(resourceInfo.getResourceClass(), resourceInfo.getResourceMethod());
    }
}