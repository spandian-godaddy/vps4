package com.godaddy.vps4.cache;

import com.google.inject.AbstractModule;

public class CacheModule extends AbstractModule {

    @Override
    public void configure() {
        bind(CacheLifecycleListener.class);
    }
}
