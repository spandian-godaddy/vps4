package com.godaddy.vps4.cache;

import javax.cache.CacheManager;
import javax.cache.configuration.MutableConfiguration;
import javax.cache.expiry.AccessedExpiryPolicy;
import javax.cache.expiry.Duration;

public class CacheSettings {

    public static void configure(CacheManager cacheManager) {

        MutableConfiguration<String, String> config = new MutableConfiguration<String, String>();
        config.setStoreByValue(true)
            .setTypes(String.class, String.class)
            .setExpiryPolicyFactory(AccessedExpiryPolicy.factoryOf(Duration.ONE_MINUTE))
            .setStatisticsEnabled(false);

        cacheManager.createCache("test", config);
    }
}
