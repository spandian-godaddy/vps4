package com.godaddy.hfs.swagger;

import java.util.Arrays;

import com.google.inject.Scopes;
import com.google.inject.multibindings.Multibinder;
import com.google.inject.servlet.ServletModule;

public class SwaggerModule extends ServletModule {

    @Override
    public void configureServlets() {
        bind(io.swagger.jaxrs.listing.ApiListingResource.class).in(Scopes.SINGLETON);
        bind(io.swagger.jaxrs.listing.SwaggerSerializers.class);

        Multibinder.newSetBinder(binder(), SwaggerClassFilter.class);

        bind(SwaggerContextListener.class);

        bindServlets();
    }

    protected void bindServlets() {
        serve("/swagger/*").with(new ClasspathStaticContentServlet(Arrays.asList("com/godaddy/swagger/assets")));
    }

}
