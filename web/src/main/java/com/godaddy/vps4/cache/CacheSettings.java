package com.godaddy.vps4.cache;

import javax.cache.CacheManager;
import javax.cache.configuration.MutableConfiguration;
import javax.cache.expiry.AccessedExpiryPolicy;
import javax.cache.expiry.Duration;

import com.godaddy.vps4.sysadmin.VmUsageService.CachedVmUsage;

public class CacheSettings {

    public static void configure(CacheManager cacheManager) {

        cacheManager.createCache("test",
                new MutableConfiguration<String, String>()
                    .setStoreByValue(true)
                    .setTypes(String.class, String.class)
                    .setExpiryPolicyFactory(
                            AccessedExpiryPolicy.factoryOf(Duration.ONE_MINUTE))
                    .setStatisticsEnabled(false));

        cacheManager.createCache("cpanel.accesshash",
                new MutableConfiguration<Long, String>()
                    .setStoreByValue(true)
                    .setTypes(Long.class, String.class)
                    .setExpiryPolicyFactory(
                            AccessedExpiryPolicy.factoryOf(Duration.ETERNAL))
                    .setStatisticsEnabled(false));

        cacheManager.createCache("vm.usage",
                new MutableConfiguration<Long, CachedVmUsage>()
                    .setStoreByValue(true)
                    .setTypes(Long.class, CachedVmUsage.class)
                    .setExpiryPolicyFactory(
                            AccessedExpiryPolicy.factoryOf(Duration.ETERNAL))
                    .setStatisticsEnabled(false));
    }
}
