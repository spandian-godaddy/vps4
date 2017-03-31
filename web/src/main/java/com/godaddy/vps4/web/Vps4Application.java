package com.godaddy.vps4.web;

import com.godaddy.hfs.servicediscovery.ZkServiceRegistrationContextListener;
import com.godaddy.hfs.swagger.SwaggerContextListener;
import com.godaddy.hfs.web.HfsWebApplication;
import com.godaddy.hfs.web.ListenerRegistration;
import com.godaddy.hfs.web.resteasy.GuiceResteasyBootstrap;
import com.godaddy.hfs.zookeeper.ZooKeeperClient;
import com.godaddy.vps4.cache.CacheLifecycleListener;
import com.google.inject.Injector;

public class Vps4Application extends HfsWebApplication {

    @Override
    public Injector newInjector() {
        return new Vps4Injector().getInstance();
    }

    @Override
    public void registerListeners(ListenerRegistration listeners) {

        listeners.addEventListener(CacheLifecycleListener.class);

        listeners.addEventListener(SwaggerContextListener.class);

        listeners.addEventListener(new GuiceResteasyBootstrap(
                beanClass -> beanClass.isAnnotationPresent(Vps4Api.class)
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
