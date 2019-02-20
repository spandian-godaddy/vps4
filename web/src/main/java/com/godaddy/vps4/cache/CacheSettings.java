package com.godaddy.vps4.cache;

import javax.cache.CacheManager;
import javax.cache.configuration.MutableConfiguration;
import javax.cache.expiry.AccessedExpiryPolicy;
import javax.cache.expiry.Duration;

import com.godaddy.hfs.vm.ServerUsageStats;
import com.godaddy.vps4.mailrelay.MailRelayService.CachedMailRelayHistory;
import com.godaddy.vps4.sysadmin.VmUsage;

public class CacheSettings {

    public static void configure(CacheManager cacheManager) {

        cacheManager.createCache("test",
                new MutableConfiguration<String, String>()
                    .setStoreByValue(true)
                    .setTypes(String.class, String.class)
                    .setExpiryPolicyFactory(
                            AccessedExpiryPolicy.factoryOf(Duration.ONE_MINUTE))
                    .setStatisticsEnabled(false));

        cacheManager.createCache(CacheName.CPANEL_ACCESSHASH,
                new MutableConfiguration<Long, String>()
                    .setStoreByValue(true)
                    .setTypes(Long.class, String.class)
                    .setExpiryPolicyFactory(
                            AccessedExpiryPolicy.factoryOf(Duration.ETERNAL))
                    .setStatisticsEnabled(false));

        cacheManager.createCache(CacheName.VM_USAGE,
                new MutableConfiguration<Long, VmUsage>()
                    .setStoreByValue(true)
                    .setTypes(Long.class, VmUsage.class)
                    .setExpiryPolicyFactory(
                            AccessedExpiryPolicy.factoryOf(Duration.ETERNAL))
                    .setStatisticsEnabled(false));

        cacheManager.createCache(CacheName.SERVER_USAGE,
                new MutableConfiguration<Long, ServerUsageStats>()
                        .setStoreByValue(true)
                        .setTypes(Long.class, ServerUsageStats.class)
                        .setExpiryPolicyFactory(
                                AccessedExpiryPolicy.factoryOf(Duration.ONE_DAY))
                        .setStatisticsEnabled(false));

        cacheManager.createCache(CacheName.MAIL_RELAY_HISTORY,
                new MutableConfiguration<String, CachedMailRelayHistory>()
                    .setStoreByValue(true)
                    .setTypes(String.class, CachedMailRelayHistory.class)
                    .setExpiryPolicyFactory(
                            AccessedExpiryPolicy.factoryOf(Duration.ONE_DAY))
                    .setStatisticsEnabled(false));
    }
}
