package com.godaddy.vps4.web.security;

import javax.ws.rs.container.DynamicFeature;
import javax.ws.rs.container.ResourceInfo;
import javax.ws.rs.core.FeatureContext;
import javax.ws.rs.ext.Provider;
import java.lang.reflect.Method;

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
        Method resourceMethod = resourceInfo.getResourceMethod();

        if (resourceMethod.isAnnotationPresent(RequiresRole.class)) {
            RequiresRoleFilter filter = getRequiresRoleFilter(resourceMethod);
            logger.info(String.format("Filter %s attached to resource class/method: [%s/%s]",
                filter.getClass().getSimpleName(), resourceInfo.getResourceClass().getSimpleName(),
                resourceInfo.getResourceMethod().getName()));

            featureContext.register(filter);
        }
    }

    private RequiresRoleFilter getRequiresRoleFilter(Method resourceMethod) {
        Role[] roles = resourceMethod.getAnnotation(RequiresRole.class).roles();
        RequiresRoleFilter filter = this.injector.getInstance(RequiresRoleFilter.class);
        filter.setReqdRoles(roles);
        return filter;
    }
}