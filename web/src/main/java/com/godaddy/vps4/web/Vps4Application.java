package com.godaddy.vps4.web;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import javax.servlet.DispatcherType;
import javax.servlet.ServletContext;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;

import org.jboss.resteasy.util.GetRestful;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.godaddy.vps4.cpanel.CpanelModule;
import com.godaddy.vps4.hfs.HfsClientModule;
import com.godaddy.vps4.hfs.HfsMockModule;
import com.godaddy.vps4.jdbc.DatabaseModule;
import com.godaddy.vps4.security.SecurityModule;
import com.godaddy.vps4.vm.VmModule;
import com.godaddy.vps4.web.network.NetworkModule;
import com.godaddy.vps4.web.security.AuthenticationFilter;
import com.godaddy.vps4.web.security.RequestAuthenticator;
import com.godaddy.vps4.web.security.Vps4UserFakeModule;
import com.godaddy.vps4.web.security.Vps4UserModule;
import com.godaddy.vps4.web.security.sso.SsoModule;
import com.godaddy.vps4.web.util.resteasy.Vps4GuiceResteasyBootstrapServletContextListener;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Module;
import com.google.inject.servlet.GuiceFilter;

import io.swagger.config.Scanner;
import io.swagger.config.ScannerFactory;

public class Vps4Application implements ServletContextListener {

    private static final Logger logger = LoggerFactory.getLogger(Vps4Application.class);

    private static boolean useFakeUser = System.getProperty("vps4.user.fake", "false").equals("true");

    final ServletContextListener resteasyListener = new Vps4GuiceResteasyBootstrapServletContextListener();

    @Override
    public void contextInitialized(ServletContextEvent sce) {

        ServletContext context = sce.getServletContext();

        Injector injector = getInjector();
        injector.injectMembers(resteasyListener);

        resteasyListener.contextInitialized(sce);

        if (!useFakeUser) {
            RequestAuthenticator requestAuthenticator = injector.getInstance(RequestAuthenticator.class);

            context.addFilter("Vps4AuthFilter", new AuthenticationFilter(requestAuthenticator))
                .addMappingForUrlPatterns(EnumSet.of(DispatcherType.REQUEST), false, "/api/*");
        }

        context.addFilter("CORS", CorsFilter.class)
            .addMappingForUrlPatterns(EnumSet.of(DispatcherType.REQUEST), false,"/api/*");

        context.addFilter("GuiceFilter", injector.getInstance(GuiceFilter.class))
            .addMappingForUrlPatterns(EnumSet.allOf(DispatcherType.class), false, "/*");

        populateSwaggerModels(injector);
    }

    @Override
    public void contextDestroyed(ServletContextEvent sce) {
        resteasyListener.contextDestroyed(sce);
    }

    private static Module getUserModule(boolean useFakeUser) {
        if (useFakeUser) {
            logger.info("USING FAKE USER");
            return new Vps4UserFakeModule();
        }
        else {
            return new Vps4UserModule();
        }
    }

    private static final Injector INJECTOR = newInjector();

    public static Injector getInjector() {
        return INJECTOR;
    }

    private static Injector newInjector() {
        List<Module> modules = new ArrayList<>();

        modules.add(new GuiceFilterModule());
        modules.add(new SwaggerModule());

        if (System.getProperty("vps4.hfs.mock", "false").equals("true")) {
            modules.add(new HfsMockModule());
            logger.info("USING MOCK HFS");
        }
        else{
            modules.add(new HfsClientModule());
        }


        modules.add(getUserModule(useFakeUser));

        modules.add(new DatabaseModule());
        modules.add(new WebModule());
        modules.add(new SecurityModule());
        modules.add(new SsoModule());

        modules.add(new VmModule());
        modules.add(new NetworkModule());
        //modules.add(new FakeCpanelModule());
        modules.add(new CpanelModule());
        modules.add(new CommandClientModule());

        return Guice.createInjector(modules);
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
