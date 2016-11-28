package com.godaddy.vps4.web;

import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.time.Duration;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import javax.servlet.DispatcherType;

import org.apache.http.impl.client.HttpClientBuilder;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.server.handler.HandlerList;
import org.eclipse.jetty.servlet.FilterHolder;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.util.thread.QueuedThreadPool;
import org.jboss.resteasy.util.GetRestful;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.godaddy.vps4.config.Config;
import com.godaddy.vps4.cpanel.FakeCpanelModule;
import com.godaddy.vps4.hfs.HfsClientModule;
import com.godaddy.vps4.hfs.HfsMockModule;
import com.godaddy.vps4.jdbc.DatabaseModule;
import com.godaddy.vps4.security.SecurityModule;
import com.godaddy.vps4.security.Vps4UserService;
import com.godaddy.vps4.vm.VmModule;
import com.godaddy.vps4.web.network.NetworkModule;
import com.godaddy.vps4.web.security.AuthenticationFilter;
import com.godaddy.vps4.web.security.Vps4RequestAuthenticator;
import com.godaddy.vps4.web.security.Vps4UserFakeModule;
import com.godaddy.vps4.web.security.Vps4UserModule;
import com.godaddy.vps4.web.security.sso.HttpKeyService;
import com.godaddy.vps4.web.security.sso.KeyService;
import com.godaddy.vps4.web.security.sso.SsoTokenExtractor;
import com.godaddy.vps4.web.util.resteasy.Vps4GuiceResteasyBootstrapServletContextListener;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Module;
import com.google.inject.servlet.GuiceFilter;

import io.swagger.config.Scanner;
import io.swagger.config.ScannerFactory;

public class WebServer {

    private static final Logger logger = LoggerFactory.getLogger(WebServer.class);

    private static boolean useFakeUser = System.getProperty("vps4.user.fake", "false").equals("true");

    public static void main(String[] args) throws Exception {
        if (args.length == 1 && args[0].equals("stop")) {
            stopServer();
        } else {
            startServer();
        }
    }

    public static void startServer() throws Exception {
        Injector injector = newInjector();

        Config conf = injector.getInstance(Config.class);

        QueuedThreadPool threadPool = new QueuedThreadPool();
        threadPool.setMaxThreads(500);

        Server server = new Server(threadPool);

        ServerConnector httpConnector = new ServerConnector(server);
        int port = Integer.parseInt(conf.get("vps4.http.port", "8080"));
        httpConnector.setPort(port);
        server.addConnector(httpConnector);

        HandlerList handlers = new HandlerList();

        handlers.addHandler(SwaggerContextHandler.newSwaggerResourceContext());
        handlers.addHandler(newHandler(conf, injector));

        server.setHandler(handlers);

        server.start();

        ServerSocket stopSocket = new ServerSocket(getStopPort(), 1, InetAddress.getLoopbackAddress());

        new Thread((Runnable)() -> {

            try {
                try {
                    logger.info("listening for shutdown on port {}", stopSocket.getLocalPort());
                    stopSocket.accept();
                    logger.info("shutdown request received, stopping server...");
                    try {
                        server.stop();
                    } catch (Exception e) {
                        logger.warn("Error stopping server", e);
                    }
                } finally {
                    stopSocket.close();
                }
            } catch (IOException e) {
                logger.error("Error listening on shutdown port", e);
            }

        }, "VPS4 Stop Listener").start();

        server.join();
    }

    static void stopServer() throws Exception {
        int stopPort = getStopPort();
        logger.info("stopping server using port {}", stopPort);
        new Socket(InetAddress.getLoopbackAddress(), stopPort).close();
    }

    static int getStopPort() {
        return Integer.parseInt(System.getProperty("hfs.http.stopPort", "9080"));
    }

    protected static Injector newInjector() {
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

        modules.add(new VmModule());
        modules.add(new NetworkModule());
        modules.add(new FakeCpanelModule());
        modules.add(new CommandClientModule());

        return Guice.createInjector(modules);
    }

    protected static ServletContextHandler newHandler(Config config, Injector injector) {

        ServletContextHandler handler = new ServletContextHandler();
        handler.setContextPath("/");
        handler.addEventListener(injector.getInstance(Vps4GuiceResteasyBootstrapServletContextListener.class));

        if (!useFakeUser)
            addAuthentication(handler, injector);

        handler.addFilter(CorsFilter.class, "/*", EnumSet.allOf(DispatcherType.class));

        FilterHolder guiceFilter = new FilterHolder(injector.getInstance(GuiceFilter.class));
        handler.addFilter(guiceFilter, "/*", EnumSet.allOf(DispatcherType.class));

        populateSwaggerModels(injector);

        return handler;
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

    private static void addAuthentication(ServletContextHandler handler, Injector injector) {

        Config conf = injector.getInstance(Config.class);


        // TODO wire up KeyService using DI
        // TODO properly configure HTTP client (max routes per host, etc)
        KeyService keyService = new HttpKeyService(conf.get("sso.url"), HttpClientBuilder.create().build());

        Vps4UserService userService = injector.getInstance(Vps4UserService.class);

        long sessionTimeoutMs = Duration.ofSeconds(
                Long.parseLong(
                        conf.get(
                                "auth.timeout",
                                String.valueOf(Duration.ofHours(24).getSeconds())))).toMillis();
        logger.info("JWT timeout: {}", sessionTimeoutMs);

        SsoTokenExtractor tokenExtractor = new SsoTokenExtractor(keyService, sessionTimeoutMs);

        handler.addFilter(
                new FilterHolder(new AuthenticationFilter(new Vps4RequestAuthenticator(tokenExtractor, userService))),
                "/*", EnumSet.of(DispatcherType.REQUEST));
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
