package com.godaddy.vps4.sysadmin;

import javax.cache.Cache;
import javax.cache.CacheManager;
import javax.ws.rs.core.Response;

import org.json.simple.JSONObject;
import org.json.simple.JSONValue;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import static org.junit.Assert.*;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.eq;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.time.Duration;
import java.time.Instant;

import com.godaddy.hfs.io.Charsets;
import com.godaddy.vps4.cache.CacheName;
import com.godaddy.vps4.sysadmin.VmUsageService.CachedVmUsage;

import gdg.hfs.vhfs.sysadmin.SysAdminService;

public class VmUsageServiceTest {

    SysAdminService sysAdminService;

    Cache<Long, CachedVmUsage> cache;

    VmUsageService vmUsageService;

    String linuxUsageJson;
    VmUsage linuxUsage;

    @Before
    public void setUp() throws Exception {
        sysAdminService = mock(SysAdminService.class);
        CacheManager cacheManager = mock(CacheManager.class);
        cache = mock(Cache.class);
        when(cacheManager.getCache(CacheName.VM_USAGE, Long.class, CachedVmUsage.class)).thenReturn(cache);

        vmUsageService = new VmUsageService(sysAdminService, cacheManager);

        // read some test data
        try (InputStream is = VmUsageParserTest.class.getResourceAsStream("usage_stats_linux.json")) {
            JSONObject json = (JSONObject)JSONValue.parse(new InputStreamReader(is, Charsets.UTF8));
            linuxUsageJson = json.toJSONString();
            linuxUsage = new VmUsageParser().parse(json);
        }
    }

    protected VmUsage makeValid(VmUsage usage) {
        usage.cpu.timestamp = Instant.now();
        usage.disk.timestamp = Instant.now();
        usage.mem.timestamp = Instant.now();
        usage.io.timestamp = Instant.now();
        return usage;
    }


    @Test
    public void testUsageNotRun() throws Exception {

        // when HFS returns that no usage has been run on the target VM...
        Response response = mock(Response.class);
        when(response.readEntity(String.class)).thenReturn("{\"data\":\"has not completed\"}");

        when(sysAdminService.usageStatsResults(42, null, null)).thenReturn(response);

        VmUsage usage = vmUsageService.getUsage(42);

        // we should have kicked off the usage stats update...
        verify(sysAdminService).usageStatsUpdate(42, 1);

        // ... but still returned an empty since we want to return
        // immediately since usage stats take a little while to gather
        assertNotNull(usage);
        assertNull(usage.io);
        assertNull(usage.cpu);
        assertNull(usage.disk);
    }

    @Test
    public void testCachedVps4() throws Exception {

        // when we already have a good cached usage for the given VM...
        when(cache.get(42L)).thenReturn(new CachedVmUsage(makeValid(linuxUsage), false));

        VmUsage usage = vmUsageService.getUsage(42);
        assertEquals(linuxUsage, usage);

        // we should not have had to call HFS for anything since
        // we had the value cached
        Mockito.verifyZeroInteractions(sysAdminService);
    }

    @Test
    public void testStaleCachedVps4() throws Exception {

        // when we have stale cached usage in the VPS4 cache...
        // (invalidate the linux usage that the cache will return)
        linuxUsage.cpu.timestamp = Instant.now().minus(Duration.ofHours(48));
        when(cache.get(42L)).thenReturn(new CachedVmUsage(linuxUsage, false));

        // ... but HFS has good (albeit stale) data
        Response response = mock(Response.class);
        when(response.getStatus()).thenReturn(202);
        when(response.readEntity(String.class)).thenReturn(this.linuxUsageJson);
        when(sysAdminService.usageStatsResults(42, null, null)).thenReturn(response);

        VmUsage usage = vmUsageService.getUsage(42);

        verify(sysAdminService).usageStatsResults(42, null, null);

        // we should use HFS's data
        assertNotNull(usage.cpu);

        // and cache the result
        ArgumentCaptor<CachedVmUsage> argument = ArgumentCaptor.forClass(CachedVmUsage.class);
        verify(cache).put(eq(42L), argument.capture());
        assertNotNull(argument.getValue().usage);

        // and since the HFS data was stale, we should be fetching new data
        assertTrue(argument.getValue().fetching);
    }

    @Test
    public void testStaleCachedHfs() throws Exception {

        // when we have stale cached usage in the VPS4 cache...
        // (invalidate the linux usage that the cache will return)
        linuxUsage.cpu.timestamp = Instant.now().minus(Duration.ofHours(48));
        when(cache.get(42L)).thenReturn(new CachedVmUsage(linuxUsage, false));

        // ... and HFS has data, but it's expired...
        Response response = mock(Response.class);
        when(response.getStatus()).thenReturn(202);

        // (remove the 'mem' field of the response to invalidate it)
        JSONObject responseJson = (JSONObject)JSONValue.parse(linuxUsageJson);
        responseJson.remove("mem");
        when(response.readEntity(String.class)).thenReturn(responseJson.toJSONString());
        when(sysAdminService.usageStatsResults(42, null, null)).thenReturn(response);

        VmUsage usage = vmUsageService.getUsage(42);

        // we should use HFS's data
        assertNotNull(usage.cpu);

        // and cache the result
        ArgumentCaptor<CachedVmUsage> argument = ArgumentCaptor.forClass(CachedVmUsage.class);
        verify(cache).put(eq(42L), argument.capture());
        assertNotNull(argument.getValue().usage);
        assertTrue(argument.getValue().fetching);

        // ... and we should have fired off a request to HFS
        //     to update the usage
        verify(sysAdminService).usageStatsResults(42, null, null);
    }


}
