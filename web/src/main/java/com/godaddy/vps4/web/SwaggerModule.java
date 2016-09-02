package com.godaddy.vps4.web;

import com.google.inject.AbstractModule;
import com.google.inject.Scopes;

public class SwaggerModule extends AbstractModule {

    @Override
    public void configure() {
        bind(io.swagger.jaxrs.listing.ApiListingResource.class).in(Scopes.SINGLETON);
        bind(io.swagger.jaxrs.listing.SwaggerSerializers.class);
    }
}
