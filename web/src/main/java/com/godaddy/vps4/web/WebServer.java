package com.godaddy.vps4.web;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import javax.servlet.DispatcherType;

import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.server.handler.HandlerList;
import org.eclipse.jetty.servlet.FilterHolder;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.util.thread.QueuedThreadPool;
import org.jboss.resteasy.plugins.guice.GuiceResteasyBootstrapServletContextListener;
import org.jboss.resteasy.plugins.server.servlet.HttpServletDispatcher;
import org.jboss.resteasy.util.GetRestful;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JSR310Module;
import com.fasterxml.jackson.jaxrs.json.JacksonJaxbJsonProvider;
import com.fasterxml.jackson.jaxrs.json.JacksonJsonProvider;
import com.godaddy.vps4.security.UserModule;
import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Module;
import com.google.inject.Scopes;
import com.google.inject.servlet.GuiceFilter;
import com.google.inject.servlet.ServletModule;

import io.swagger.config.Scanner;
import io.swagger.config.ScannerFactory;

public class WebServer {

	public static void main(String[] args) throws Exception {

		QueuedThreadPool threadPool = new QueuedThreadPool();
	    threadPool.setMaxThreads(500);

	    Server server = new Server(threadPool);

	    ServerConnector httpConnector = new ServerConnector(server);
	    httpConnector.setPort(8080);
	    server.addConnector(httpConnector);

	    HandlerList handlers = new HandlerList();

	    handlers.addHandler(SwaggerContextHandler.newSwaggerResourceContext());
	    handlers.addHandler(newHandler());

	    server.setHandler(handlers);

        server.start();
        server.join();
	}

	protected static ServletContextHandler newHandler() {

        List<Module> modules = new ArrayList<>();

        modules.add(new ServletModule() {
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
                bind(GuiceResteasyBootstrapServletContextListener.class);

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
        });

        modules.add(new UserModule());
        modules.add(new AbstractModule() {
            @Override
            public void configure() {
                bind(UsersResource.class);

                bind(io.swagger.jaxrs.listing.ApiListingResource.class).in(Scopes.SINGLETON);
                bind(io.swagger.jaxrs.listing.SwaggerSerializers.class);

            }
        });

        Injector injector = Guice.createInjector(modules);

        ServletContextHandler handler = new ServletContextHandler();
        handler.setContextPath("/");
        handler.addEventListener(injector.getInstance(GuiceResteasyBootstrapServletContextListener.class));

        FilterHolder guiceFilter = new FilterHolder(injector.getInstance(GuiceFilter.class));
        handler.addFilter(guiceFilter,  "/*",  EnumSet.allOf(DispatcherType.class));

        populateSwaggerModels(injector);

        return handler;
	}

	static void populateSwaggerModels(Injector injector) {

	    // extract JAX-RS classes from injector
        final Set<Class<?>> appClasses = injector.getAllBindings().keySet().stream()
            .map(key -> key.getTypeLiteral().getRawType())
            .filter(GetRestful::isRootResource)
            .distinct()
            .collect(Collectors.toSet());

        ScannerFactory.setScanner(new Scanner() {
            @Override
            public Set<Class<?>> classes() {
                return appClasses;
            }

            @Override
            public boolean getPrettyPrint() {
                return false;
            }

            @Override
            public void setPrettyPrint(boolean shouldPrettyPrint) {
                // ignore
            }
        });
	}

}
