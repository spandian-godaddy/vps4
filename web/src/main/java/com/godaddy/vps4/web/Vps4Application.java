package com.godaddy.vps4.web;

import java.util.EnumSet;
import java.util.Set;
import java.util.stream.Collectors;

import javax.servlet.DispatcherType;
import javax.servlet.ServletContext;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;

import org.jboss.resteasy.util.GetRestful;

import com.godaddy.hfs.web.HfsWebApplication;
import com.godaddy.hfs.web.resteasy.HfsGuiceResteasyBootstrapServletContextListener;
import com.google.inject.Injector;
import com.google.inject.servlet.GuiceFilter;

import io.swagger.config.Scanner;
import io.swagger.config.ScannerFactory;

public class Vps4Application implements HfsWebApplication {

    final ServletContextListener resteasyListener;

    public Vps4Application() {
        resteasyListener = new HfsGuiceResteasyBootstrapServletContextListener(
                beanClass -> beanClass.isAnnotationPresent(Vps4Api.class)
                            || beanClass.getName().startsWith("io.swagger")
        );
    }

    @Override
    public void contextInitialized(ServletContextEvent sce) {

        ServletContext context = sce.getServletContext();

        Injector injector = new Vps4Injector().getInstance();
        injector.injectMembers(resteasyListener);

        resteasyListener.contextInitialized(sce);

        context.addFilter("GuiceFilter",
                injector.getInstance(GuiceFilter.class))
                .addMappingForUrlPatterns(EnumSet.allOf(DispatcherType.class), true, "/*");

        populateSwaggerModels(injector);
    }

    @Override
    public void contextDestroyed(ServletContextEvent sce) {
        resteasyListener.contextDestroyed(sce);
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
