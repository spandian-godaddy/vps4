package com.godaddy.vps4.web;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;

import javax.servlet.DispatcherType;

import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.servlet.FilterHolder;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.util.thread.QueuedThreadPool;
import org.jboss.resteasy.plugins.guice.GuiceResteasyBootstrapServletContextListener;
import org.jboss.resteasy.plugins.server.servlet.HttpServletDispatcher;

import com.fasterxml.jackson.jaxrs.json.JacksonJsonProvider;
import com.godaddy.vps4.security.UserModule;
import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Module;
import com.google.inject.Scopes;
import com.google.inject.servlet.GuiceFilter;
import com.google.inject.servlet.ServletModule;

public class WebServer {

	public static void main(String[] args) throws Exception {

		QueuedThreadPool threadPool = new QueuedThreadPool();
	    threadPool.setMaxThreads(500);

	    Server server = new Server(threadPool);

	    ServerConnector httpConnector = new ServerConnector(server);
	    httpConnector.setPort(8080);
	    server.addConnector(httpConnector);

	    server.setHandler(newHandler());

        server.start();
        server.join();
	}

	protected static ServletContextHandler newHandler() {

        List<Module> modules = new ArrayList<>();

        modules.add(new AbstractModule() {
            @Override
            protected void configure() {

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
            }
        });

        modules.add(new ServletModule() {
            @Override
            protected void configureServlets() {

                // hook Jackson into Jersey as the POJO <-> JSON mapper
                JacksonJsonProvider jsonProvider = new JacksonJsonProvider();
                // TODO configure jsonProvider with our preferred settings

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
            }
        });

        Injector injector = Guice.createInjector(modules);

        ServletContextHandler handler = new ServletContextHandler();
        handler.setContextPath("/");
        handler.addEventListener(injector.getInstance(GuiceResteasyBootstrapServletContextListener.class));

        FilterHolder guiceFilter = new FilterHolder(injector.getInstance(GuiceFilter.class));
        handler.addFilter(guiceFilter,  "/*",  EnumSet.allOf(DispatcherType.class));

        return handler;
	}
}
