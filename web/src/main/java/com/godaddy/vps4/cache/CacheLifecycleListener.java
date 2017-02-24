package com.godaddy.vps4.cache;

import javax.cache.CacheManager;
import javax.cache.spi.CachingProvider;
import javax.inject.Inject;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CacheLifecycleListener implements ServletContextListener {

    private static final Logger logger = LoggerFactory.getLogger(CacheLifecycleListener.class);

    final CachingProvider cachingProvider;

    @Inject
    public CacheLifecycleListener(CachingProvider cachingProvider) {
        this.cachingProvider = cachingProvider;
    }

    @Override
    public void contextInitialized(ServletContextEvent sce) {

        logger.info("initializing cache provider");

        // configure the caches
        CacheManager cacheManager = cachingProvider.getCacheManager();

        CacheSettings.configure(cacheManager);
    }

    @Override
    public void contextDestroyed(ServletContextEvent sce) {

        logger.info("destroying cache provider");
        cachingProvider.close();
    }


}
