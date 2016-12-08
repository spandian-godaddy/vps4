package com.godaddy.vps4.web;

import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;

import org.apache.curator.framework.CuratorFramework;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.server.handler.HandlerList;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.util.thread.QueuedThreadPool;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.godaddy.vps4.config.Config;
import com.godaddy.vps4.util.ZooKeeperClient;
import com.godaddy.vps4.web.servicediscovery.ServiceRegistration;
import com.godaddy.vps4.web.servicediscovery.ServiceRegistrationContextListener;
import com.google.inject.Injector;

public class WebServer {

    private static final Logger logger = LoggerFactory.getLogger(WebServer.class);

    public static void main(String[] args) throws Exception {
        if (args.length == 1 && args[0].equals("stop")) {
            stopServer();
        } else {
            startServer();
        }
    }

    public static void startServer() throws Exception {
        Injector injector = Vps4Application.getInjector();

        Config conf = injector.getInstance(Config.class);

        QueuedThreadPool threadPool = new QueuedThreadPool();
        threadPool.setMaxThreads(500);

        Server server = new Server(threadPool);

        ServerConnector httpConnector = new ServerConnector(server);
        int port = Integer.parseInt(conf.get("vps4.http.port", "8080"));
        httpConnector.setPort(port);
        server.addConnector(httpConnector);

        HandlerList handlers = new HandlerList();

        // TODO bind Swagger static content into a listener
        handlers.addHandler(SwaggerContextHandler.newSwaggerResourceContext());

        ServletContextHandler handler = new ServletContextHandler();
        handler.setContextPath("/");

        // TODO bind with ServiceLoader
        handler.addEventListener(new Vps4Application());

        boolean serviceDiscoveryEnabled = Boolean.parseBoolean(conf.get("servicediscovery.enabled", "false"));
        if (serviceDiscoveryEnabled) {
            CuratorFramework zk = ZooKeeperClient.getInstance();

            ServiceRegistration serviceRegistration = new ServiceRegistration();
            serviceRegistration.address = resolveHostname();
            serviceRegistration.locations.add("/api/");
            serviceRegistration.name = "api";
            serviceRegistration.port = port;
            serviceRegistration.sslPort = 0;

            String path = conf.get("servicediscovery.zk.path");

            handler.addEventListener(new ServiceRegistrationContextListener(serviceRegistration, path, zk));
        }

        handlers.addHandler(handler);

        server.setHandler(handlers);

        server.start();

        listenForStop(server);

        server.join();
    }

    static void listenForStop(Server server) throws IOException {
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
    }

    static void stopServer() throws Exception {
        int stopPort = getStopPort();
        logger.info("stopping server using port {}", stopPort);
        new Socket(InetAddress.getLoopbackAddress(), stopPort).close();
    }

    static int getStopPort() {
        return Integer.parseInt(System.getProperty("hfs.http.stopPort", "9081"));
    }

    protected static String resolveHostname() {
        try {
            return InetAddress.getLocalHost().getHostName();
        }
        catch (UnknownHostException e) {
            throw new RuntimeException("Unable to determine local hostname", e);
        }
    }

}
