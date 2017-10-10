package com.godaddy.vps4.scheduler;

import ch.qos.logback.classic.Level;
import com.godaddy.hfs.servicediscovery.ZkServiceRegistrationContextListener;
import com.godaddy.hfs.swagger.SwaggerContextListener;
import com.godaddy.hfs.web.HfsWebApplication;
import com.godaddy.hfs.web.ListenerRegistration;
import com.godaddy.hfs.web.resteasy.GuiceResteasyBootstrap;
import com.godaddy.hfs.zookeeper.ZooKeeperClient;
import com.godaddy.vps4.scheduler.web.Vps4SchedulerApi;
import com.google.inject.Injector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Vps4SchedulerMain extends HfsWebApplication {

    private static final Logger logger = LoggerFactory.getLogger(Vps4SchedulerMain.class);

    @Override
    public Injector newInjector() {
        return new Vps4SchedulerInjector().getInstance();
    }

    @Override
    public void registerListeners(ListenerRegistration listeners) {

        listeners.addEventListener(SwaggerContextListener.class);

        listeners.addEventListener(new GuiceResteasyBootstrap(
                beanClass -> beanClass.isAnnotationPresent(Vps4SchedulerApi.class)
                        || beanClass.getName().startsWith("io.swagger")
        ));

        if (ZooKeeperClient.isConfigured()) {
            listeners.addEventListener(ZkServiceRegistrationContextListener.class);
        }

        listeners.addEventListener(SchedulerContextListener.class);
    }


    public static void main(String[] args) {
        // Added Ability to set log level for kafka pacakges to debug issues if any.
        ch.qos.logback.classic.Logger shutupJetty = (ch.qos.logback.classic.Logger) org.slf4j.LoggerFactory.getLogger("org.eclipse.jetty");
        shutupJetty.setLevel(Level.INFO);
        ch.qos.logback.classic.Logger shutupSwagger = (ch.qos.logback.classic.Logger) org.slf4j.LoggerFactory.getLogger("io.swagger");
        shutupSwagger.setLevel(Level.INFO);
        ch.qos.logback.classic.Logger shutupQuartz = (ch.qos.logback.classic.Logger) org.slf4j.LoggerFactory.getLogger("org.quartz");
        shutupQuartz.setLevel(Level.INFO);
        ch.qos.logback.classic.Logger shutupGoogle = (ch.qos.logback.classic.Logger) org.slf4j.LoggerFactory.getLogger("com.google");
        shutupGoogle.setLevel(Level.INFO);
        ch.qos.logback.classic.Logger shutupJboss = (ch.qos.logback.classic.Logger) org.slf4j.LoggerFactory.getLogger("org.jboss");
        shutupJboss.setLevel(Level.INFO);

        new Vps4SchedulerMain().run(args);
    }


}
