package com.godaddy.hfs.web;

import java.util.EnumSet;
import java.util.Set;

import javax.servlet.DispatcherType;
import javax.servlet.ServletContextListener;

import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.servlet.FilterHolder;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.godaddy.hfs.servicediscovery.HfsServiceMetadata;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Key;
import com.google.inject.Module;
import com.google.inject.TypeLiteral;
import com.google.inject.multibindings.OptionalBinder;
import com.google.inject.servlet.GuiceFilter;


public abstract class HfsWebApplication {

    private static final Logger logger = LoggerFactory.getLogger(HfsWebApplication.class);
    
    Injector injector;

    public HfsWebApplication() {

    }

    protected final Injector getInjector() {
        Injector injector = this.injector;
              
        if (injector == null) {            
            injector = this.injector = newInjector();
        }
        
        return injector;
    }

    protected Injector newInjector() {
        return Guice.createInjector(binder -> {
            OptionalBinder.newOptionalBinder(binder, HfsServiceMetadata.class);
            binder.install(getApplicationModule());
        });
    }
    
    protected Module getApplicationModule() {
        throw new RuntimeException("getApplicationModule() or newInjector() must be overridden!");
    }
    
    protected void registerListeners(ListenerRegistration listeners) {
        logger.info("no listeners registered");
    }

    public void run(String[] args) {

        start();
    }

    public void start() {
        Injector injector = getInjector();

        // create server
        Server server = injector.getInstance(Server.class);

        // add registered connectors
        Set<ServerConnector> connectors = injector.getInstance(Key.get(new TypeLiteral<Set<ServerConnector>>(){}));
        for (ServerConnector connector : connectors) {
            logger.debug("Adding connector for protocol {}, port {} to Jetty server", String.join(",", connector.getProtocols()), connector.getLocalPort());
            server.addConnector(connector);
        }

        // handler
        ServletContextHandler handler = new ServletContextHandler();
        handler.setContextPath("/");

        registerListeners(new ListenerRegistration() {

            @Override
            public void addEventListener(ServletContextListener listener) {
                injector.injectMembers(listener);
                handler.addEventListener(listener);
            }

            @Override
            public void addEventListener(Class<? extends ServletContextListener> listenerClass) {
                handler.addEventListener(injector.getInstance(listenerClass));
            }
        });

        // serve through Guice
        handler.addFilter(
                new FilterHolder(injector.getInstance(GuiceFilter.class)),
                "/*",
                EnumSet.allOf(DispatcherType.class));
        
        server.setHandler(handler);

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            try {
                server.stop();
            } catch (Exception e) {
                logger.error("Error calling server.stop()", e);
            }
        }));

        try {
            server.start();

            server.join();

        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

}
