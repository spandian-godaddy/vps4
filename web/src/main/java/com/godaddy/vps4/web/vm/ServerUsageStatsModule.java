package com.godaddy.vps4.web.vm;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.cache.CacheManager;

import com.godaddy.hfs.vm.VmService;
import com.godaddy.vps4.vm.ServerUsageStatsService;
import com.google.inject.AbstractModule;
import com.google.inject.Provides;

public class ServerUsageStatsModule extends AbstractModule
{
    @Override
    protected void configure() {
    }

    @Provides
    public ServerUsageStatsService provideServerUsageStatsService(VmService vmService, CacheManager cacheManager) {
        ExecutorService updateCacheExecutorService = Executors.newCachedThreadPool();
        return new ServerUsageStatsService(vmService, cacheManager, updateCacheExecutorService);
    }

}
