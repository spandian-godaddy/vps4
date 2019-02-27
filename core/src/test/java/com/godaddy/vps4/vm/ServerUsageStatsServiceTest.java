package com.godaddy.vps4.vm;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyLong;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.UUID;
import java.util.concurrent.ExecutorService;

import javax.cache.Cache;
import javax.cache.CacheManager;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.runners.MockitoJUnitRunner;

import com.godaddy.hfs.vm.ServerUsageStats;
import com.godaddy.hfs.vm.VmService;
import com.godaddy.vps4.cache.CacheName;

@RunWith(MockitoJUnitRunner.class)
public class ServerUsageStatsServiceTest {

    private VmService vmService = mock(VmService.class);
    private CacheManager cacheManager = mock(CacheManager.class);
    private Cache<Long, ServerUsageStats> cache = mock(Cache.class);
    private ExecutorService updateCacheExecutorService = mock(ExecutorService.class);
    private ServerUsageStatsService serverUsageStatsService;

    @Before
    public void setUp() throws Exception {
        when(cacheManager.getCache(CacheName.SERVER_USAGE, Long.class, ServerUsageStats.class)).thenReturn(cache);
        serverUsageStatsService = new ServerUsageStatsService(vmService, cacheManager, updateCacheExecutorService);
    }

    @After
    public void tearDown() throws Exception {
        serverUsageStatsService = null;
    }

    private ServerUsageStats createFakeServerUsageStats() {
        ServerUsageStats fakeServerUsageStats = new ServerUsageStats();
        fakeServerUsageStats.setVmId(1234L);
        fakeServerUsageStats.setCollected(ZonedDateTime.now());
        fakeServerUsageStats.setRequested(ZonedDateTime.now().minus(1, ChronoUnit.MINUTES));
        fakeServerUsageStats.setDiskTotal(100L);
        fakeServerUsageStats.setDiskUsed(90L);
        fakeServerUsageStats.setMemoryTotal(100L);
        fakeServerUsageStats.setMemoryUsed(90L);
        fakeServerUsageStats.setCpuUsed(0.5);
        fakeServerUsageStats.setUtilizationId(100L);
        return fakeServerUsageStats;
    }

    @Test
    public void fetchesStatsForCacheMiss() {
        serverUsageStatsService = new ServerUsageStatsService(vmService, cacheManager, updateCacheExecutorService);

        ServerUsageStats fakeServerUsageStats = createFakeServerUsageStats();
        when(cache.get(anyLong())).thenReturn(null);
        when(vmService.updateServerUsageStats(anyLong())).thenReturn(fakeServerUsageStats);
        when(vmService.getServerUsageStats(anyLong(), anyLong())).thenReturn(fakeServerUsageStats);
        doNothing().when(cache).put(anyLong(), any(ServerUsageStats.class));

        serverUsageStatsService.getServerUsage(1234L);

        verify(vmService, times(1)).updateServerUsageStats(anyLong());
        verify(cache, atLeastOnce()).put(anyLong(), eq(fakeServerUsageStats));
    }

    @Test
    public void doesNotFetchStatsForCacheHits() {
        ServerUsageStats fakeServerUsageStats = createFakeServerUsageStats();
        serverUsageStatsService = new ServerUsageStatsService(vmService, cacheManager, updateCacheExecutorService);
        when(cache.get(anyLong())).thenReturn(fakeServerUsageStats);

        serverUsageStatsService.getServerUsage(1234L);

        verify(vmService, never()).updateServerUsageStats(anyLong());
        verify(vmService, never()).getServerUsageStats(anyLong(), anyLong());
        verify(cache, never()).put(anyLong(), eq(fakeServerUsageStats));
    }

    /**
     * Create a fake object instance for ServerUsageStats where collected time is null.
     * This forces a refresh of the stats from HFS. (underlying platform service that provides stats for the server)
     *
     * @return ServerUsageStats object
     */
    private ServerUsageStats createForceWaitForStats() {
        return new ServerUsageStats(100L,
                1234L, ZonedDateTime.now(), UUID.randomUUID().toString(),
                null, 0L, 0L, 0L, 0L, 0.0);
    }

    @Test
    public void submitsAsyncTaskToRefreshStats() {
        serverUsageStatsService = new ServerUsageStatsService(vmService, cacheManager, updateCacheExecutorService);

        ServerUsageStats fakeServerUsageStats = createFakeServerUsageStats();
        when(cache.get(anyLong())).thenReturn(null);
        when(vmService.updateServerUsageStats(anyLong())).thenReturn(fakeServerUsageStats);

        serverUsageStatsService.getServerUsage(1234L);

        verify(vmService, times(1)).updateServerUsageStats(anyLong());
        verify(updateCacheExecutorService, atLeastOnce()).execute(any(Runnable.class));
    }

    @Test
    public void returnsUtilizationIdAfterTimeout() {
        serverUsageStatsService = new ServerUsageStatsService(vmService, cacheManager, updateCacheExecutorService);

        ServerUsageStats forceWaitForStats = createForceWaitForStats();
        when(cache.get(anyLong())).thenReturn(null);
        when(vmService.updateServerUsageStats(anyLong())).thenReturn(forceWaitForStats);
        ServerUsageStats serverUsageStats = serverUsageStatsService.getServerUsage(1234L);

        verify(vmService, atLeastOnce()).updateServerUsageStats(anyLong());
        verify(updateCacheExecutorService, atLeastOnce()).execute(any(Runnable.class));
        assertEquals("Usage stats object does not match utilization id as expected. ", 100L, serverUsageStats.getUtilizationId());
    }

    @Test
    public void failfastIfStatsCannotBeFetched() {
        serverUsageStatsService = new ServerUsageStatsService(vmService, cacheManager, updateCacheExecutorService);

        when(cache.get(anyLong())).thenReturn(null);
        when(vmService.updateServerUsageStats(anyLong())).thenReturn(null);

        ServerUsageStats stats = serverUsageStatsService.getServerUsage(1234L);

        verify(vmService, atLeastOnce()).updateServerUsageStats(anyLong());
        verify(vmService, never()).getServerUsageStats(anyLong(), eq(100L));
        verify(cache, never()).put(anyLong(), any(ServerUsageStats.class));
        assertTrue("Expected usage stats object to be null. ", stats == null);
    }

    private ServerUsageStats createStaleServerUsageStats() {
        ServerUsageStats staleServerUsageStats = new ServerUsageStats();
        staleServerUsageStats.setVmId(1234L);
        staleServerUsageStats.setCollected(ZonedDateTime.now().minus(6, ChronoUnit.MINUTES));
        staleServerUsageStats.setRequested(ZonedDateTime.now().minus(10, ChronoUnit.MINUTES));
        staleServerUsageStats.setDiskTotal(100);
        staleServerUsageStats.setDiskUsed(90);
        staleServerUsageStats.setMemoryTotal(100);
        staleServerUsageStats.setMemoryUsed(90);
        staleServerUsageStats.setCpuUsed(0.5);
        staleServerUsageStats.setUtilizationId(100);
        return staleServerUsageStats;
    }

    @Test
    public void refreshesStaleStatsFromCacheHits() {
        serverUsageStatsService = new ServerUsageStatsService(vmService, cacheManager, updateCacheExecutorService);

        ServerUsageStats staleServerUsageStats = createStaleServerUsageStats();
        when(cache.get(anyLong())).thenReturn(staleServerUsageStats);
        ServerUsageStats fakeServerUsageStats = createFakeServerUsageStats();
        when(vmService.updateServerUsageStats(anyLong())).thenReturn(fakeServerUsageStats);
        when(vmService.getServerUsageStats(anyLong(), anyLong())).thenReturn(fakeServerUsageStats);
        doNothing().when(cache).put(anyLong(), any(ServerUsageStats.class));

        serverUsageStatsService.getServerUsage(1234L);

        verify(vmService, times(1)).updateServerUsageStats(anyLong());
        verify(cache, atLeastOnce()).put(anyLong(), eq(fakeServerUsageStats));
        verify(cache, never()).put(anyLong(), eq(staleServerUsageStats));
    }

    private ServerUsageStats createFailedRefreshUsageStats() {
        ServerUsageStats failedRefreshUsageStats = new ServerUsageStats();
        failedRefreshUsageStats.setVmId(1234L);
        failedRefreshUsageStats.setCollected(ZonedDateTime.now().minus(10, ChronoUnit.MINUTES));
        failedRefreshUsageStats.setRequested(ZonedDateTime.now().minus(6, ChronoUnit.MINUTES));
        failedRefreshUsageStats.setDiskTotal(100);
        failedRefreshUsageStats.setDiskUsed(90);
        failedRefreshUsageStats.setMemoryTotal(100);
        failedRefreshUsageStats.setMemoryUsed(90);
        failedRefreshUsageStats.setCpuUsed(0.5);
        failedRefreshUsageStats.setUtilizationId(100);
        return failedRefreshUsageStats;
    }

    @Test
    public void updatesStatsForPreviousRefreshTimeout() {
        ServerUsageStats failedRefreshUsageStats = createFailedRefreshUsageStats();
        ServerUsageStats fakeServerUsageStats = createFakeServerUsageStats();
        when(cache.get(anyLong())).thenReturn(failedRefreshUsageStats);
        when(vmService.updateServerUsageStats(anyLong())).thenReturn(fakeServerUsageStats);
        when(vmService.getServerUsageStats(anyLong(), anyLong())).thenReturn(fakeServerUsageStats);
        doNothing().when(cache).put(anyLong(), any(ServerUsageStats.class));

        serverUsageStatsService.getServerUsage(1234L);

        verify(vmService, times(1)).updateServerUsageStats(anyLong());
        verify(cache, atLeastOnce()).put(anyLong(), eq(fakeServerUsageStats));
    }
}