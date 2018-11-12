package com.godaddy.vps4.sysadmin;

import static org.mockito.Matchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.time.Duration;
import java.time.Instant;

import javax.cache.Cache;
import javax.cache.CacheManager;
import javax.ws.rs.core.Response;

import org.json.simple.JSONObject;
import org.json.simple.JSONValue;
import org.junit.Before;
import org.junit.Test;

import com.godaddy.hfs.io.Charsets;
import com.godaddy.vps4.cache.CacheName;
import com.godaddy.vps4.util.TimestampUtils;

import gdg.hfs.vhfs.sysadmin.SysAdminAction;
import gdg.hfs.vhfs.sysadmin.SysAdminService;
import junit.framework.Assert;

public class VmUsageServiceTest {

    SysAdminService sysAdminService;
    VmUsageService vmUsageService;
    Cache<Long, VmUsage> cache;

    Long hfsVmId = 42L;
    String linuxUsageJson;

    @Before
    public void setUp() throws Exception {
        sysAdminService = mock(SysAdminService.class);
        CacheManager cacheManager = mock(CacheManager.class);
        cache = mock(Cache.class);
        when(cacheManager.getCache(CacheName.VM_USAGE, Long.class, VmUsage.class)).thenReturn(cache);
        vmUsageService = new VmUsageService(sysAdminService, cacheManager);

        // read some test data
        try (InputStream is = VmUsageParserTest.class.getResourceAsStream("usage_stats_linux.json")) {
            JSONObject json = (JSONObject)JSONValue.parse(new InputStreamReader(is, Charsets.UTF8));
            linuxUsageJson = json.toJSONString();
        }
    }

    @Test
    public void testGetUsageWithNoCachedValue() throws Exception {
        SysAdminAction hfsStatsUpdate = mock(SysAdminAction.class);
        when(sysAdminService.usageStatsUpdate(hfsVmId, 0)).thenReturn(hfsStatsUpdate);
        when(cache.get(hfsVmId)).thenReturn(null);

        VmUsage usage = vmUsageService.getUsage(hfsVmId);
        verify(sysAdminService, never()).getSysAdminAction(anyLong());
        verify(sysAdminService, never()).usageStatsResults(hfsVmId, null, null);
        verify(sysAdminService).usageStatsUpdate(hfsVmId, 0);
        verify(cache).put(hfsVmId, usage);
        Assert.assertEquals(hfsStatsUpdate.sysAdminActionId, usage.pendingHfsActionId);
        Assert.assertNull(usage.cpu);
        Assert.assertNull(usage.mem);
        Assert.assertNull(usage.disk);
        Assert.assertNull(usage.io);
    }

    @Test
    public void testGetUsageRefreshComplete() throws Exception {
        VmUsage cachedUsage = new VmUsage();
        cachedUsage.pendingHfsActionId = 42L;
        when(cache.get(hfsVmId)).thenReturn(cachedUsage);

        SysAdminAction hfsAction = mock(SysAdminAction.class);
        hfsAction.status = SysAdminAction.Status.COMPLETE;
        hfsAction.completedAt = "2017-02-09 17:40:02";
        when(sysAdminService.getSysAdminAction(cachedUsage.pendingHfsActionId)).thenReturn(hfsAction);

        Response response = mock(Response.class);
        when(response.getStatusInfo()).thenReturn(Response.Status.OK);
        when(response.readEntity(String.class)).thenReturn(linuxUsageJson);
        when(sysAdminService.usageStatsResults(42, null, null)).thenReturn(response);

        VmUsage usage = vmUsageService.getUsage(hfsVmId);
        verify(sysAdminService, never()).usageStatsUpdate(hfsVmId, 0);
        verify(sysAdminService).getSysAdminAction(42L);
        verify(sysAdminService).usageStatsResults(hfsVmId, null, null);
        Assert.assertEquals(TimestampUtils.parseHfsTimestamp(hfsAction.completedAt),
                usage.refreshedAt);
        Assert.assertNotNull(usage.canRefreshAgainAt());
        Assert.assertEquals(0, usage.pendingHfsActionId);
        verify(cache).put(hfsVmId, usage);
    }

    @Test
    public void testGetUsageRefreshInProgressTimedOut() throws Exception {
        VmUsage cachedUsage = new VmUsage();
        cachedUsage.pendingHfsActionId = 42L;
        when(cache.get(hfsVmId)).thenReturn(cachedUsage);

        SysAdminAction hfsAction = mock(SysAdminAction.class);
        hfsAction.status = SysAdminAction.Status.IN_PROGRESS;
        hfsAction.createdAt = "2010-01-01 00:01:01.000001";  // a time long long ago to represent a timed out request in progress
        when(sysAdminService.getSysAdminAction(cachedUsage.pendingHfsActionId)).thenReturn(hfsAction);
        SysAdminAction hfsStatsUpdate = mock(SysAdminAction.class);
        hfsStatsUpdate.sysAdminActionId = 123321;
        when(sysAdminService.usageStatsUpdate(hfsVmId, 0)).thenReturn(hfsStatsUpdate);

        VmUsage usage = vmUsageService.getUsage(hfsVmId);
        verify(sysAdminService).usageStatsUpdate(hfsVmId, 0);
        verify(sysAdminService, never()).usageStatsResults(hfsVmId, null, null);
        verify(sysAdminService).getSysAdminAction(42L);
        Assert.assertNull(usage.refreshedAt);
        Assert.assertEquals(hfsStatsUpdate.sysAdminActionId, usage.pendingHfsActionId);
        verify(cache).put(hfsVmId, usage);
    }

    @Test
    public void testGetUsageRefreshInProgress() throws Exception {
        VmUsage cachedUsage = new VmUsage();
        cachedUsage.pendingHfsActionId = 42L;
        when(cache.get(hfsVmId)).thenReturn(cachedUsage);

        SysAdminAction hfsAction = mock(SysAdminAction.class);
        hfsAction.status = SysAdminAction.Status.IN_PROGRESS;
        hfsAction.createdAt = "2148-01-01 00:01:01.000001";  // a time when vps4 is long gone to represent an in progress request that hasn't timed out
        when(sysAdminService.getSysAdminAction(cachedUsage.pendingHfsActionId)).thenReturn(hfsAction);

        VmUsage usage = vmUsageService.getUsage(hfsVmId);
        verify(sysAdminService, never()).usageStatsUpdate(hfsVmId, 0);
        verify(sysAdminService, never()).usageStatsResults(hfsVmId, null, null);
        verify(sysAdminService).getSysAdminAction(42L);
        Assert.assertNull(usage.refreshedAt);
        Assert.assertEquals(42L, usage.pendingHfsActionId);
        verify(cache).put(hfsVmId, usage);
    }

    @Test
    public void testGetUsageCannotRefreshYet() throws Exception {
        VmUsage cachedUsage = new VmUsage();
        cachedUsage.refreshedAt = Instant.now().minus(Duration.ofMinutes(9));
        when(cache.get(hfsVmId)).thenReturn(cachedUsage);

        VmUsage usage = vmUsageService.getUsage(hfsVmId);
        verify(sysAdminService, never()).usageStatsUpdate(hfsVmId, 0);
        verify(sysAdminService, never()).usageStatsResults(hfsVmId, null, null);
        verify(sysAdminService, never()).getSysAdminAction(42L);
        Assert.assertEquals(cachedUsage.refreshedAt, usage.refreshedAt);
        Assert.assertEquals(0, usage.pendingHfsActionId);
        verify(cache).put(hfsVmId, usage);
    }

    @Test
    public void testGetUsageCanRefresh() throws Exception {
        VmUsage cachedUsage = new VmUsage();
        cachedUsage.refreshedAt = Instant.now().minus(Duration.ofMinutes(11));
        when(cache.get(hfsVmId)).thenReturn(cachedUsage);

        SysAdminAction hfsStatsUpdate = mock(SysAdminAction.class);
        when(sysAdminService.usageStatsUpdate(hfsVmId, 0)).thenReturn(hfsStatsUpdate);

        VmUsage usage = vmUsageService.getUsage(hfsVmId);
        verify(sysAdminService, never()).getSysAdminAction(anyLong());
        verify(sysAdminService, never()).usageStatsResults(hfsVmId, null, null);
        verify(sysAdminService).usageStatsUpdate(hfsVmId, 0);
        verify(cache).put(hfsVmId, usage);
        Assert.assertEquals(hfsStatsUpdate.sysAdminActionId, usage.pendingHfsActionId);
    }

    @Test
    public void testGetUsageHfsFetchFailed() throws Exception {
        VmUsage cachedUsage = new VmUsage();
        cachedUsage.pendingHfsActionId = 42L;
        when(cache.get(hfsVmId)).thenReturn(cachedUsage);

        SysAdminAction hfsAction = mock(SysAdminAction.class);
        hfsAction.status = SysAdminAction.Status.FAILED;
        hfsAction.completedAt = "2017-02-09 17:40:02";
        when(sysAdminService.getSysAdminAction(cachedUsage.pendingHfsActionId)).thenReturn(hfsAction);

        Response response = mock(Response.class);
        when(response.getStatusInfo()).thenReturn(Response.Status.OK);
        when(response.readEntity(String.class)).thenReturn("{\"data\":\"has not completed\"}");
        when(sysAdminService.usageStatsResults(42, null, null)).thenReturn(response);

        VmUsage usage = vmUsageService.getUsage(hfsVmId);
        System.out.println(usage.toString());
        verify(sysAdminService).getSysAdminAction(42L);
        verify(sysAdminService).usageStatsResults(hfsVmId, null, null);
        Assert.assertEquals(0, usage.pendingHfsActionId);
        Assert.assertNull(usage.cpu);
        Assert.assertNull(usage.mem);
        Assert.assertNull(usage.disk);
        Assert.assertNull(usage.io);
    }

    @Test
    public void testGetUsageHfsResponse404() throws Exception {
        VmUsage cachedUsage = new VmUsage();
        cachedUsage.pendingHfsActionId = 42L;
        when(cache.get(hfsVmId)).thenReturn(cachedUsage);

        SysAdminAction hfsAction = mock(SysAdminAction.class);
        hfsAction.status = SysAdminAction.Status.FAILED;
        hfsAction.completedAt = "2017-02-09 17:40:02";
        when(sysAdminService.getSysAdminAction(cachedUsage.pendingHfsActionId)).thenReturn(hfsAction);

        Response response = mock(Response.class);
        when(response.getStatusInfo()).thenReturn(Response.Status.NOT_FOUND);
        when(sysAdminService.usageStatsResults(42, null, null)).thenReturn(response);

        VmUsage usage = vmUsageService.getUsage(hfsVmId);
        Assert.assertNull(usage.cpu);
        Assert.assertNull(usage.mem);
        Assert.assertNull(usage.disk);
        Assert.assertNull(usage.io);
    }
}
