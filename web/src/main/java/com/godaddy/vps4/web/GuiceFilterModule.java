package com.godaddy.vps4.web;

import org.jboss.resteasy.plugins.server.servlet.HttpServletDispatcher;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JSR310Module;
import com.fasterxml.jackson.jaxrs.json.JacksonJaxbJsonProvider;
import com.fasterxml.jackson.jaxrs.json.JacksonJsonProvider;
import com.godaddy.vps4.web.util.resteasy.Vps4GuiceResteasyBootstrapServletContextListener;
import com.google.inject.Scopes;
import com.google.inject.servlet.GuiceFilter;
import com.google.inject.servlet.ServletModule;

public class GuiceFilterModule extends ServletModule {

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
        bind(Vps4GuiceResteasyBootstrapServletContextListener.class);

        // hook Jackson into Jersey as the POJO <-> JSON mapper
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JSR310Module());
        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

        JacksonJsonProvider jsonProvider = new JacksonJaxbJsonProvider(mapper, JacksonJaxbJsonProvider.DEFAULT_ANNOTATIONS);
        jsonProvider.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

        bind(JacksonJsonProvider.class).toInstance(jsonProvider);

        // all requests go through RESTEasy dispatcher
        bind(HttpServletDispatcher.class).in(Scopes.SINGLETON);
        serve("/*").with(HttpServletDispatcher.class);

        bind(DefaultExceptionMapper.class);
    }
}
