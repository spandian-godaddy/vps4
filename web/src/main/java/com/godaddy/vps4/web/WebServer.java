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
import org.jboss.resteasy.util.GetRestful;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.godaddy.vps4.config.Config;
import com.godaddy.vps4.config.ConfigProvider;
import com.godaddy.vps4.jdbc.DatabaseModule;
import com.godaddy.vps4.security.Vps4UserModule;
import com.godaddy.vps4.web.cpanel.CpanelModule;
import com.godaddy.vps4.web.cpanel.FakeCpanelModule;
import com.godaddy.vps4.web.hfs.HfsMockModule;
import com.godaddy.vps4.web.hfs.HfsModule;
import com.godaddy.vps4.web.network.NetworkModule;
import com.godaddy.vps4.vm.VmModule;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Module;
import com.google.inject.servlet.GuiceFilter;

import io.swagger.config.Scanner;
import io.swagger.config.ScannerFactory;

public class WebServer {


    private static final Logger logger = LoggerFactory.getLogger(WebServer.class);

    private static int getPortFromConfig(){
        Config conf = new ConfigProvider().get();
        return Integer.valueOf(conf.get("vps4.http.port", "8080"));
    }

	public static void main(String[] args) throws Exception {

		QueuedThreadPool threadPool = new QueuedThreadPool();
	    threadPool.setMaxThreads(500);

	    Server server = new Server(threadPool);

	    ServerConnector httpConnector = new ServerConnector(server);
	    int port = getPortFromConfig();
	    httpConnector.setPort(port);
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

        modules.add(new GuiceFilterModule());
        modules.add(new SwaggerModule());

        if(System.getProperty("vps4.hfs.mock", "false").equals("true")) {
            logger.info("USING MOCK HFS");
            modules.add(new HfsMockModule());
        } else {
            modules.add(new HfsModule());
        }

        modules.add(new DatabaseModule());
        modules.add(new WebModule());
        modules.add(new Vps4UserModule());

        modules.add(new VmModule());
        modules.add(new NetworkModule());
        //modules.add(new CPanelModule());
        modules.add(new FakeCpanelModule());

        Injector injector = Guice.createInjector(modules);

        ServletContextHandler handler = new ServletContextHandler();
        handler.setContextPath("/");
        handler.addEventListener(injector.getInstance(GuiceResteasyBootstrapServletContextListener.class));

        handler.addFilter(CorsFilter.class, "/*", EnumSet.allOf(DispatcherType.class));

        FilterHolder guiceFilter = new FilterHolder(injector.getInstance(GuiceFilter.class));
        handler.addFilter(guiceFilter,  "/*",  EnumSet.allOf(DispatcherType.class));

        populateSwaggerModels(injector);

        return handler;
	}

	static boolean isVps4Api(Class<?> resourceClass) {
	    return resourceClass.isAnnotationPresent(Vps4Api.class);
	}

	static void populateSwaggerModels(Injector injector) {

	    // extract JAX-RS classes from injector
        final Set<Class<?>> appClasses = injector.getAllBindings().keySet().stream()
            .map(key -> key.getTypeLiteral().getRawType())
            .filter(WebServer::isVps4Api)
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
