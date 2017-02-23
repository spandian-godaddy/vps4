package com.godaddy.vps4.web;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.EnumSet;
import java.util.Set;
import java.util.stream.Collectors;

import javax.servlet.DispatcherType;
import javax.servlet.ServletContext;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;

import org.jboss.resteasy.util.GetRestful;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.godaddy.hfs.web.HfsWebApplication;
import com.google.inject.Injector;
import com.google.inject.Key;
import com.google.inject.TypeLiteral;
import com.google.inject.servlet.GuiceFilter;

import io.swagger.config.Scanner;
import io.swagger.config.ScannerFactory;

public class Vps4Application implements HfsWebApplication {

    private static final Logger logger = LoggerFactory.getLogger(Vps4Application.class);

    final Deque<ServletContextListener> listenerDeque = new ArrayDeque<>();

    public Vps4Application() {

    }

    @Override
    public void contextInitialized(ServletContextEvent sce) {

        ServletContext context = sce.getServletContext();

        Injector injector = new Vps4Injector().getInstance();

        Set<ServletContextListener> listeners = injector.getInstance(Key.get(new TypeLiteral<Set<ServletContextListener>>(){}));
        try {
            initializeListeners(listeners, sce);
        } catch (Exception e) {
            logger.error("Error executing context listener (destroying existing listeners and exiting)", e);
            contextDestroyed(null);
        }

        context.addFilter("GuiceFilter",
                injector.getInstance(GuiceFilter.class))
                .addMappingForUrlPatterns(EnumSet.allOf(DispatcherType.class), true, "/*");

        populateSwaggerModels(injector);
    }

    protected void initializeListeners(Set<ServletContextListener> listeners, ServletContextEvent sce) {
        for (ServletContextListener listener : listeners) {

            // some listeners may require injection
            //injector.injectMembers(listener);

            listener.contextInitialized(sce);
            listenerDeque.push(listener);
        }
    }

    @Override
    public void contextDestroyed(ServletContextEvent sce) {
        for (ServletContextListener listener : listenerDeque) {
            try {
                listener.contextDestroyed(sce);
            } catch (Exception e) {
                logger.error("Error destroying context listener (continuing to next listener)", e);
            }
        }
    }



    static boolean isVps4Api(Class<?> resourceClass) {
        return resourceClass.isAnnotationPresent(Vps4Api.class);
    }

    static void populateSwaggerModels(Injector injector) {

        // extract JAX-RS classes from injector
        final Set<Class<?>> appClasses = injector.getAllBindings().keySet().stream()
                .map(key -> key.getTypeLiteral().getRawType())
                .filter(Vps4Application::isVps4Api)
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
