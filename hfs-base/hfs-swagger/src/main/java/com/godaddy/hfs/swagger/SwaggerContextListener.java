package com.godaddy.hfs.swagger;

import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.inject.Inject;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;

import com.google.inject.Injector;

import io.swagger.config.Scanner;

public class SwaggerContextListener implements ServletContextListener {

    @Inject
    Injector injector;

    @Inject
    Set<SwaggerClassFilter> filters;

    @Override
    public void contextInitialized(ServletContextEvent sce) {

        Stream<Class<?>> classStream = injector.getAllBindings().keySet().stream()
                                    .map(key -> key.getTypeLiteral().getRawType());

        if (filters != null) {
            for (SwaggerClassFilter filter : filters) {
                classStream = classStream.filter(filter);
            }
        }

        final Set<Class<?>> appClasses = classStream
                .distinct()
                .collect(Collectors.toSet());

        Scanner scanner = new Scanner() {
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
        };

        sce.getServletContext().setAttribute("swagger.scanner.id.default", scanner);
    }

    @Override
    public void contextDestroyed(ServletContextEvent sce) {

    }

}
