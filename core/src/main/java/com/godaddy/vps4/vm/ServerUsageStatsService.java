package com.godaddy.vps4.vm;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;

import javax.cache.Cache;
import javax.cache.CacheManager;
import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.godaddy.hfs.vm.ServerUsageStats;
import com.godaddy.hfs.vm.VmService;
import com.godaddy.vps4.cache.CacheName;

public class ServerUsageStatsService {

    private static final Logger logger = LoggerFactory.getLogger(ServerUsageStatsService.class);
    private final VmService vmService;
    private final Cache<Long, ServerUsageStats> cache;
    private ExecutorService updateCacheExecutorService;
    private long sleepInterval = 2000; // 2 secs
    private long timeout = 60000; // 60 secs

    @Inject
    public ServerUsageStatsService(VmService vmService, CacheManager cacheManager, ExecutorService updateCacheExecutorService) {
        this.vmService = vmService;
        this.cache = cacheManager.getCache(CacheName.SERVER_USAGE, Long.class, ServerUsageStats.class);
        this.updateCacheExecutorService = updateCacheExecutorService;
    }

    public ServerUsageStats getServerUsage(long hfsVmId) {

        ServerUsageStats usageStats = cache.get(hfsVmId);
        if (usageStats == null) {
            return refreshUsageStats(hfsVmId);
        }
        if (usageStats.areStale()) {
            return refreshUsageStats(hfsVmId);
        }

        return usageStats;
    }

    private ServerUsageStats refreshUsageStats(long hfsVmId) {

        ServerUsageStats serverUsageStats = vmService.updateServerUsageStats(hfsVmId);
        if(serverUsageStats == null) {
            return null;
        }

        // ensure cache is updated with the pending update record.
        cache.put(hfsVmId, serverUsageStats);

        // spin off a separate process to update the cache with the refreshed data.
        CompletableFuture.supplyAsync(() -> getUpdatedUsageStats(hfsVmId, serverUsageStats), updateCacheExecutorService)
                .thenApply(updatedServerUsageStats -> {
                    cache.put(hfsVmId, updatedServerUsageStats);
                    return updatedServerUsageStats;
                });

        // return existing usage stats with utilization id, so client can attempt another refresh
        return serverUsageStats;
    }

    ServerUsageStats getUpdatedUsageStats(long hfsVmId, ServerUsageStats serverUsageStats) {
        // loop for 60 secs to see if we can refresh the vm usage stats. (timeout set to 60 secs)
        long startTime = System.currentTimeMillis();
        while (hasNotTimedOut(startTime)) {

            try {

                ServerUsageStats updatedServerUsageStats = vmService.getServerUsageStats(hfsVmId, serverUsageStats.getUtilizationId());
                if (updatedServerUsageStats != null) {
                    if (updatedServerUsageStats.getCollected() != null) {
                        // success! we have refreshed stats; we should send them back to be put in the cache.
                        logger.info("Success! Stats refreshed: " + updatedServerUsageStats.toString());
                        return updatedServerUsageStats;
                    }
                }

                logger.info("Sleeping for " + sleepInterval + "ms while server usage stats are made available... ");
                sleep();

            } catch (InterruptedException e) {
                logger.info("Thread interrupted sleep while getting usage stats populated for hfs vm id: %s ", hfsVmId);
            }
        }
        return serverUsageStats;
    }

    private void sleep() throws InterruptedException {
        Thread.sleep(sleepInterval);
    }

    private boolean hasNotTimedOut(long startTime) {
        return (System.currentTimeMillis() - startTime) < timeout;
    }
}
