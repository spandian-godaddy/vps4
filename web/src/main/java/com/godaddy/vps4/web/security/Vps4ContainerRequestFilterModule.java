package com.godaddy.vps4.web.security;

import com.google.inject.AbstractModule;

public class Vps4ContainerRequestFilterModule extends AbstractModule {
    @Override
    protected void configure() {
        bind(AdminAuthFilter.class);
        bind(RequiresRoleFilter.class);
        bind(RequiresRoleFeature.class);
        bind(TemporarilyDisabledFeature.class);
        bind(TemporarilyDisabledEndpointFilter.class);
        bind(BlockForServerTypeFeature.class);
        bind(BlockForServerTypeFilter.class);
        bind(LookupVmFeature.class);
        bind(LookupVmFilter.class);
//        bind(ShopperVmAccessFeature.class);
//        bind(ShopperVmAccessOkFilter.class);
    }
}
