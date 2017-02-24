package com.godaddy.vps4.cache;

import javax.cache.CacheManager;
import javax.cache.spi.CachingProvider;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.hazelcast.cache.impl.HazelcastServerCachingProvider;
import com.hazelcast.core.HazelcastInstance;

public class HazelcastCacheModule extends AbstractModule {

    @Override
    public void configure() {

        bind(HazelcastInstance.class).toProvider(HazelcastProvider.class);
    }

    @Provides
    public static CachingProvider provideCacheManager(HazelcastInstance hazelcastInstance) {
        return HazelcastServerCachingProvider.createCachingProvider(hazelcastInstance);
    }

    @Provides
    public static CacheManager provideCacheManager(CachingProvider cachingProvider) {
        return cachingProvider.getCacheManager();
    }
}
