package com.godaddy.hfs.servicediscovery;

import javax.inject.Singleton;

import com.google.inject.AbstractModule;

public class ZkServiceRegistrationModule extends AbstractModule {

    @Override
    public void configure() {
        bind(ZkServiceRegistrationContextListener.class)
            .in(Singleton.class);
    }

}
