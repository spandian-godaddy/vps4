package com.godaddy.vps4.scheduler.util;

import com.godaddy.hfs.swagger.SwaggerClassFilter;
import com.godaddy.hfs.web.DefaultExceptionMapper;
import com.godaddy.vps4.scheduler.web.Vps4SchedulerApi;
import com.google.inject.Scopes;
import com.google.inject.multibindings.Multibinder;
import com.google.inject.servlet.GuiceFilter;
import com.google.inject.servlet.ServletModule;
import org.jboss.resteasy.plugins.server.servlet.HttpServletDispatcher;

public class GuiceFilterModule extends ServletModule {

    final String[] paths;

    public GuiceFilterModule(String...paths) {
        this.paths = paths;
    }

    @Override
    protected void configureServlets() {

        bind(GuiceFilter.class).in(Scopes.SINGLETON);

        // all requests go through RESTEasy dispatcher
        bind(HttpServletDispatcher.class).in(Scopes.SINGLETON);
        for (String path : paths) {
            serve(path).with(HttpServletDispatcher.class);
        }

        bind(DefaultExceptionMapper.class);

        // this is to prevent Swagger from discovering Jax-rs resources exposed by non-Vps4Scheduler apis
        Multibinder.newSetBinder(binder(), SwaggerClassFilter.class)
                .addBinding().toInstance(resourceClass ->
                resourceClass.isAnnotationPresent(Vps4SchedulerApi.class));
    }
}