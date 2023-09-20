package com.godaddy.vps4.web.security;

import javax.ws.rs.container.DynamicFeature;
import javax.ws.rs.container.ResourceInfo;
import javax.ws.rs.core.FeatureContext;
import javax.ws.rs.ext.Provider;

import com.godaddy.vps4.web.Vps4Api;
import com.godaddy.vps4.web.security.GDUser.Role;
import com.google.inject.Inject;
import com.google.inject.Injector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Provider
public class RequiresRoleFeature implements DynamicFeature {

    private static final Logger logger = LoggerFactory.getLogger(RequiresRoleFeature.class);

    Injector injector;

    @Inject
    public RequiresRoleFeature(Injector injector) {
        this.injector = injector;
    }

    @Override
    public void configure(ResourceInfo resourceInfo, FeatureContext featureContext) {
        if (isVps4Api(resourceInfo)) {
            RequiresRoleFilter filter = getRequiresRoleFilter(resourceInfo);
            logger.info(String.format("Filter %s attached to resource class/method: [%s/%s]",
                filter.getClass().getSimpleName(), resourceInfo.getResourceClass().getSimpleName(),
                resourceInfo.getResourceMethod().getName()));

            featureContext.register(filter);
        }
    }

    private boolean isVps4Api(ResourceInfo resourceInfo) {
        return resourceInfo.getResourceClass().isAnnotationPresent(Vps4Api.class);
    }

    private boolean hasRequiresRoleAnnotation(ResourceInfo resourceInfo) {
        return resourceInfo.getResourceMethod().isAnnotationPresent(RequiresRole.class)
            || resourceInfo.getResourceClass().isAnnotationPresent(RequiresRole.class);
    }

    private RequiresRoleFilter getRequiresRoleFilter(ResourceInfo resourceInfo) {
        Role[] roles = new Role[]{Role.ADMIN, Role.CUSTOMER};
        if(hasRequiresRoleAnnotation(resourceInfo)) {
            roles = resourceInfo.getResourceMethod().isAnnotationPresent(RequiresRole.class)
                    ? resourceInfo.getResourceMethod().getAnnotation(RequiresRole.class).roles() // use the annotation on the resource method if present
                    : resourceInfo.getResourceClass().getAnnotation(RequiresRole.class).roles(); // else use the annotation on the resource class
        }
        RequiresRoleFilter filter = this.injector.getInstance(RequiresRoleFilter.class);
        filter.setReqdRoles(roles);
        return filter;
    }
}