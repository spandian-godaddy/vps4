package com.godaddy.hfs.web;

import javax.inject.Inject;
import javax.inject.Provider;

import org.jboss.resteasy.plugins.server.servlet.HttpServletDispatcher;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.jaxrs.json.JacksonJaxbJsonProvider;
import com.fasterxml.jackson.jaxrs.json.JacksonJsonProvider;
import com.google.inject.Scopes;
import com.google.inject.servlet.GuiceFilter;
import com.google.inject.servlet.ServletModule;

public class GuiceFilterModule extends ServletModule {

    final String[] paths;

    public GuiceFilterModule() {
        this("/*");
    }

    public GuiceFilterModule(String...paths) {
        this.paths = paths;
    }

    @Override
    protected void configureServlets() {

        // only inject objects we explicitly bind,
        // don't attempt runtime introspection/JIT binding
        binder().requireExplicitBindings();

        // explicitly bind GuiceFilter so we can an instance of
        // it from the injector later with all our resources injected
        bind(GuiceFilter.class).in(Scopes.SINGLETON);

        // by binding the servlet context listener here, the 'parentInjector'
        // field is injected by Guice.  That parentInjector is what contains
        // all of our application-level bindings
        //bind(Vps4GuiceResteasyBootstrapServletContextListener.class);

        // hook Jackson into Jersey as the POJO <-> JSON mapper

        bind(JacksonJsonProvider.class).toProvider(JacksonJsonProviderProvider.class);

        // all requests go through RESTEasy dispatcher
        bind(HttpServletDispatcher.class).in(Scopes.SINGLETON);
        for (String path : paths) {
            serve(path).with(HttpServletDispatcher.class);
        }

        bind(DefaultExceptionMapper.class);
    }

    static class JacksonJsonProviderProvider implements Provider<JacksonJsonProvider> {

        @Inject
        ObjectMapper mapper;

        @Override
        public JacksonJsonProvider get() {

            JacksonJsonProvider jsonProvider = new JacksonJaxbJsonProvider(mapper, JacksonJaxbJsonProvider.DEFAULT_ANNOTATIONS);
            return jsonProvider;
        }

    }

}
