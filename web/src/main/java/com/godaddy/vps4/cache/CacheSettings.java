package com.godaddy.vps4.cache;

import javax.cache.CacheManager;
import javax.cache.configuration.MutableConfiguration;
import javax.cache.expiry.AccessedExpiryPolicy;
import javax.cache.expiry.CreatedExpiryPolicy;
import javax.cache.expiry.Duration;

import com.godaddy.hfs.vm.ServerUsageStats;
import com.godaddy.vps4.mailrelay.MailRelayService.CachedMailRelayHistory;
import com.godaddy.vps4.panopta.DefaultPanoptaService;
import com.godaddy.hfs.vm.HfsInventoryDataWrapper;

public class CacheSettings {

    public static void configure(CacheManager cacheManager) {

        cacheManager.createCache("test",
                new MutableConfiguration<String, String>()
                    .setStoreByValue(true)
                    .setTypes(String.class, String.class)
                    .setExpiryPolicyFactory(
                            AccessedExpiryPolicy.factoryOf(Duration.ONE_MINUTE))
                    .setStatisticsEnabled(false));

        cacheManager.createCache(CacheName.API_JWT_TOKENS,
                new MutableConfiguration<String, String>()
                    .setStoreByValue(true)
                    .setTypes(String.class, String.class)
                    .setExpiryPolicyFactory(
                            CreatedExpiryPolicy.factoryOf(Duration.TEN_MINUTES))
                    .setStatisticsEnabled(false));

        cacheManager.createCache(CacheName.CPANEL_API_TOKEN,
                new MutableConfiguration<Long, String>()
                        .setStoreByValue(true)
                        .setTypes(Long.class, String.class)
                        .setExpiryPolicyFactory(
                                AccessedExpiryPolicy.factoryOf(Duration.ETERNAL))
                        .setStatisticsEnabled(false));

        cacheManager.createCache(CacheName.CPANEL_API_TOKEN_ACTION,
                new MutableConfiguration<Long, Long>()
                        .setStoreByValue(true)
                        .setTypes(Long.class, Long.class)
                        .setExpiryPolicyFactory(
                                CreatedExpiryPolicy.factoryOf(Duration.TEN_MINUTES))
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
                            AccessedExpiryPolicy.factoryOf(Duration.FIVE_MINUTES))
                    .setStatisticsEnabled(false));

        cacheManager.createCache(CacheName.PANOPTA_METRIC_GRAPH,
                new MutableConfiguration<String, DefaultPanoptaService.CachedMonitoringGraphs>()
                    .setStoreByValue(true)
                    .setTypes(String.class, DefaultPanoptaService.CachedMonitoringGraphs.class)
                    .setExpiryPolicyFactory(
                            CreatedExpiryPolicy.factoryOf(Duration.FIVE_MINUTES))
                    .setStatisticsEnabled(false));

        cacheManager.createCache(CacheName.OVH_INVENTORY,
                new MutableConfiguration<String, HfsInventoryDataWrapper>()
                    .setStoreByValue(true)
                    .setTypes(String.class, HfsInventoryDataWrapper.class)
                    .setExpiryPolicyFactory(
                            CreatedExpiryPolicy.factoryOf(Duration.ONE_MINUTE))
                    .setStatisticsEnabled(false));
    }
}
