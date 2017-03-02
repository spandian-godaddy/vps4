package com.godaddy.vps4.cache;

import javax.cache.CacheManager;
import javax.cache.configuration.MutableConfiguration;
import javax.cache.expiry.AccessedExpiryPolicy;
import javax.cache.expiry.Duration;

import com.godaddy.vps4.mailrelay.MailRelayService.CachedMailRelayHistory;
import com.godaddy.vps4.mailrelay.MailRelayService.CachedMailRelayUsage;
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

        cacheManager.createCache(CacheName.CPANEL_ACCESSHASH,
                new MutableConfiguration<Long, String>()
                    .setStoreByValue(true)
                    .setTypes(Long.class, String.class)
                    .setExpiryPolicyFactory(
                            AccessedExpiryPolicy.factoryOf(Duration.ETERNAL))
                    .setStatisticsEnabled(false));

        cacheManager.createCache(CacheName.VM_USAGE,
                new MutableConfiguration<Long, CachedVmUsage>()
                    .setStoreByValue(true)
                    .setTypes(Long.class, CachedVmUsage.class)
                    .setExpiryPolicyFactory(
                            AccessedExpiryPolicy.factoryOf(Duration.ETERNAL))
                    .setStatisticsEnabled(false));
        
        cacheManager.createCache(CacheName.MAIL_RELAY_USAGE,
                new MutableConfiguration<String, CachedMailRelayUsage>()
                    .setStoreByValue(true)
                    .setTypes(String.class, CachedMailRelayUsage.class)
                    .setExpiryPolicyFactory(
                            AccessedExpiryPolicy.factoryOf(Duration.ONE_MINUTE))
                    .setStatisticsEnabled(false));
        
        cacheManager.createCache(CacheName.MAIL_RELAY_HISTORY,
                new MutableConfiguration<String, CachedMailRelayHistory>()
                    .setStoreByValue(true)
                    .setTypes(String.class, CachedMailRelayHistory.class)
                    .setExpiryPolicyFactory(
                            AccessedExpiryPolicy.factoryOf(Duration.ONE_MINUTE))
                    .setStatisticsEnabled(false));
    }
}
