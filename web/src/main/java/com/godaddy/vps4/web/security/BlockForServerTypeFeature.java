package com.godaddy.vps4.web.security;

import com.godaddy.vps4.vm.ServerType.Type;
import com.google.inject.Inject;
import com.google.inject.Injector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.container.DynamicFeature;
import javax.ws.rs.container.ResourceInfo;
import javax.ws.rs.core.FeatureContext;
import javax.ws.rs.ext.Provider;


@Provider
public class BlockForServerTypeFeature implements DynamicFeature {

    private static final Logger logger = LoggerFactory.getLogger(BlockForServerTypeFeature.class);

    Injector injector;

    @Inject
    public BlockForServerTypeFeature(Injector injector) {
        this.injector = injector;
    }

    @Override
    public void configure(ResourceInfo resourceInfo, FeatureContext featureContext) {
        if (isBlockByServerTypeFilterNeeded(resourceInfo)) {
            BlockForServerTypeFilter filter = getBlockForServerTypeFilter(resourceInfo);
            logger.info(String.format("Filter %s attached to resource class/method: [%s/%s]",
                filter.getClass().getSimpleName(), resourceInfo.getResourceClass().getSimpleName(),
                resourceInfo.getResourceMethod().getName()));

            featureContext.register(filter);
        }
    }

    private boolean isBlockByServerTypeFilterNeeded(ResourceInfo resourceInfo) {
        return resourceInfo.getResourceMethod().isAnnotationPresent(BlockServerType.class)
            || resourceInfo.getResourceClass().isAnnotationPresent(BlockServerType.class);
    }

    private BlockForServerTypeFilter getBlockForServerTypeFilter(ResourceInfo resourceInfo) {
        Type[] serverTypes = resourceInfo.getResourceMethod().isAnnotationPresent(BlockServerType.class)
            ? resourceInfo.getResourceMethod().getAnnotation(BlockServerType.class).serverTypes() // use the annotation on the resource method if present
            : resourceInfo.getResourceClass().getAnnotation(BlockServerType.class).serverTypes(); // else use the annotation on the resource class

        BlockForServerTypeFilter filter = this.injector.getInstance(BlockForServerTypeFilter.class);
        filter.setServerTypesToBlock(serverTypes);
        return filter;
    }
}