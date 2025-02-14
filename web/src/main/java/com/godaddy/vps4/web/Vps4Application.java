package com.godaddy.vps4.web;

import com.godaddy.hfs.servicediscovery.ZkServiceRegistrationContextListener;
import com.godaddy.hfs.swagger.SwaggerContextListener;
import com.godaddy.hfs.web.HfsWebApplication;
import com.godaddy.hfs.web.ListenerRegistration;
import com.godaddy.hfs.web.resteasy.GuiceResteasyBootstrap;
import com.godaddy.hfs.zookeeper.ZooKeeperClient;
import com.godaddy.vps4.cache.CacheLifecycleListener;
import com.godaddy.vps4.web.log.LogLevelListener;
import com.google.inject.Injector;

import gdg.hfs.orchestration.web.CommandsResource;
import gdg.hfs.orchestration.web.CommandsViewResource;
import org.eclipse.jetty.server.ConnectionFactory;
import org.eclipse.jetty.server.HttpConfiguration;
import org.eclipse.jetty.server.HttpConnectionFactory;
import org.eclipse.jetty.server.ServerConnector;

public class Vps4Application extends HfsWebApplication {
    private int maxHeaderSize = 16384;

    @Override
    public Injector newInjector() {
        return new Vps4Injector().getInstance();
    }

    @Override
    public ServerConnector configureConnector(ServerConnector connector) {
        HttpConfiguration httpConfig = new HttpConfiguration();
        httpConfig.setRequestHeaderSize(maxHeaderSize);
        ConnectionFactory connectionFactory = new HttpConnectionFactory(httpConfig);
        connector.addConnectionFactory(connectionFactory);
        return connector;
    }

    @Override
    public void registerListeners(ListenerRegistration listeners) {

        listeners.addEventListener(LogLevelListener.class);

        listeners.addEventListener(CacheLifecycleListener.class);

        listeners.addEventListener(SwaggerContextListener.class);

        listeners.addEventListener(new GuiceResteasyBootstrap(
                beanClass -> beanClass.isAnnotationPresent(Vps4Api.class)
                            || beanClass.isAssignableFrom(CommandsResource.class)
                            || beanClass.isAssignableFrom(CommandsViewResource.class)
                            || beanClass.getName().startsWith("io.swagger")

        ));

        // conditionally add service registration last, since the service
        // should only be registered once all other listeners have executed
        if (ZooKeeperClient.isConfigured()) {
            listeners.addEventListener(ZkServiceRegistrationContextListener.class);
        }
    }

    public static void main(String[] args) {
        new Vps4Application().run(args);
    }

}
