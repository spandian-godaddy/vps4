package com.godaddy.vps4.sysadmin;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.text.ParseException;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;

import javax.cache.Cache;
import javax.cache.CacheManager;
import javax.ws.rs.core.Response;

import org.json.simple.JSONObject;
import org.json.simple.JSONValue;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.godaddy.hfs.config.Config;
import com.godaddy.hfs.io.Charsets;
import com.godaddy.vps4.cache.CacheName;
import com.godaddy.vps4.cache.HazelcastProvider;
import com.godaddy.vps4.sysadmin.VmUsageService.CachedVmUsage;
import com.godaddy.vps4.util.TimeStamp;

import gdg.hfs.vhfs.sysadmin.SysAdminAction;
import gdg.hfs.vhfs.sysadmin.SysAdminService;

public class VmUsageServiceTest {

    SysAdminService sysAdminService;

    Cache<Long, CachedVmUsage> cache;

    VmUsageService vmUsageService;

    String linuxUsageJson;
    VmUsage linuxUsage;

    String hddOnlyUsageJson;
    VmUsage hddOnlyUsage;

    @Before
    public void setUp() throws Exception {
        sysAdminService = mock(SysAdminService.class);
        CacheManager cacheManager = mock(CacheManager.class);
        cache = mock(Cache.class);
        when(cacheManager.getCache(CacheName.VM_USAGE, Long.class, CachedVmUsage.class)).thenReturn(cache);
        Config config = mock(Config.class);
        when(config.get("hfs.sysadmin.secondsToWaitForStatsUpdate")).thenReturn("300");
        vmUsageService = new VmUsageService(config, sysAdminService, cacheManager);

        // read some test data
        try (InputStream is = VmUsageParserTest.class.getResourceAsStream("usage_stats_linux.json")) {
            JSONObject json = (JSONObject)JSONValue.parse(new InputStreamReader(is, Charsets.UTF8));
            linuxUsageJson = json.toJSONString();
            linuxUsage = new VmUsageParser().parse(json);
        }

        try (InputStream is = VmUsageParserTest.class.getResourceAsStream("usage_stats_hdd_only.json")) {
            JSONObject json = (JSONObject)JSONValue.parse(new InputStreamReader(is, Charsets.UTF8));
            hddOnlyUsageJson = json.toJSONString();
            hddOnlyUsage = new VmUsageParser().parse(json);
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
    public void testSerialization() throws Exception {
        ObjectMapper mapper = HazelcastProvider.newObjectMapper();

        String json = mapper.writeValueAsString(new CachedVmUsage(this.linuxUsage, 1));
        CachedVmUsage cachedUsage = (CachedVmUsage)mapper.readValue(json, Object.class);
        assertNotNull(cachedUsage);
        assertNotNull(cachedUsage.usage);
        assertEquals(1, cachedUsage.updateActionId);
    }

    @Test
    public void testHddOnlyInitialUsage() throws Exception {

        // when HFS returns that no usage has been run on the target VM...
        Response response = mock(Response.class);
        when(response.readEntity(String.class)).thenReturn(hddOnlyUsageJson);

        when(sysAdminService.usageStatsResults(42, null, null)).thenReturn(response);

        SysAdminAction action = new SysAdminAction();
        action.vmId = 42;
        action.status = SysAdminAction.Status.COMPLETE;

        when(sysAdminService.usageStatsUpdate(42, 1)).thenReturn(action);

        VmUsage usage = vmUsageService.getUsage(42);
        usage = vmUsageService.getUsage(42);

        // we should have kicked off the usage stats update...
        verify(sysAdminService, Mockito.times(2)).usageStatsUpdate(42, 1);

        // ... but still returned an empty since we want to return
        // immediately since usage stats take a little while to gather
        assertNotNull(usage);
        assertNull(usage.io);
        assertNull(usage.cpu);
        assertNull(usage.disk);
    }

    @Test
    public void testUsageNotRun() throws Exception {

        // when HFS returns that no usage has been run on the target VM...
        Response response = mock(Response.class);
        when(response.readEntity(String.class)).thenReturn("{\"data\":\"has not completed\"}");

        SysAdminAction action = new SysAdminAction();
        action.vmId = 42;
        action.status = SysAdminAction.Status.COMPLETE;

        when(sysAdminService.usageStatsUpdate(42, 1)).thenReturn(action);
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
        when(cache.get(42L)).thenReturn(new CachedVmUsage(makeValid(linuxUsage), -1));

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
        when(cache.get(42L)).thenReturn(new CachedVmUsage(linuxUsage, -1));

        // ... but HFS has good (albeit stale) data
        Response response = mock(Response.class);
        when(response.getStatus()).thenReturn(202);
        when(response.readEntity(String.class)).thenReturn(this.linuxUsageJson);
        when(sysAdminService.usageStatsResults(42, null, null)).thenReturn(response);

        SysAdminAction action = new SysAdminAction();
        action.vmId = 42;
        action.status = SysAdminAction.Status.COMPLETE;

        when(sysAdminService.usageStatsUpdate(42, 1)).thenReturn(action);

        VmUsage usage = vmUsageService.getUsage(42);

        verify(sysAdminService).usageStatsResults(42, null, null);

        // we should use HFS's data
        assertNotNull(usage.cpu);

        // and cache the result
        ArgumentCaptor<CachedVmUsage> argument = ArgumentCaptor.forClass(CachedVmUsage.class);
        verify(cache).put(eq(42L), argument.capture());
        assertNotNull(argument.getValue().usage);

        // and since the HFS data was stale, we should be fetching new data
        assertEquals(0, argument.getValue().updateActionId);
    }

    @Test
    public void testStaleCachedHfs() throws Exception {

        // when we have stale cached usage in the VPS4 cache...
        // (invalidate the linux usage that the cache will return)
        linuxUsage.cpu.timestamp = Instant.now().minus(Duration.ofHours(48));
        when(cache.get(42L)).thenReturn(new CachedVmUsage(linuxUsage, -1));

        // ... and HFS has data, but it's expired...
        Response response = mock(Response.class);
        when(response.getStatus()).thenReturn(202);

        // (remove the 'mem' field of the response to invalidate it)
        JSONObject responseJson = (JSONObject)JSONValue.parse(linuxUsageJson);
        responseJson.remove("mem");
        when(response.readEntity(String.class)).thenReturn(responseJson.toJSONString());
        when(sysAdminService.usageStatsResults(42, null, null)).thenReturn(response);

        SysAdminAction action = new SysAdminAction();
        action.vmId = 42;
        action.status = SysAdminAction.Status.COMPLETE;

        when(sysAdminService.usageStatsUpdate(42, 1)).thenReturn(action);

        VmUsage usage = vmUsageService.getUsage(42);

        // we should use HFS's data
        assertNotNull(usage.cpu);

        // and cache the result
        ArgumentCaptor<CachedVmUsage> argument = ArgumentCaptor.forClass(CachedVmUsage.class);
        verify(cache).put(eq(42L), argument.capture());
        assertNotNull(argument.getValue().usage);
        assertEquals(0, argument.getValue().updateActionId);

        // ... and we should have fired off a request to HFS
        //     to update the usage
        verify(sysAdminService).usageStatsResults(42, null, null);
    }

    @Test
    public void testHfsActionNotCompleting() throws ParseException {
        when(cache.get(42L)).thenReturn(new CachedVmUsage(null, 1));

        Response response = mock(Response.class);
        when(response.getStatus()).thenReturn(202);

        String jsonResponse = "{\"data\": \"has not completed\"}";
        when(response.readEntity(String.class)).thenReturn(jsonResponse);
        when(sysAdminService.usageStatsResults(42, null, null)).thenReturn(response);

        SysAdminAction beforeAction = new SysAdminAction();
        beforeAction.vmId = 42;
        beforeAction.sysAdminActionId = 1;
        beforeAction.status = SysAdminAction.Status.IN_PROGRESS;
        beforeAction.createdAt = ZonedDateTime.now(ZoneId.of("UTC")).minusMinutes(6).format(TimeStamp.hfsActionTimestampFormat);

        SysAdminAction afterAction = new SysAdminAction();
        afterAction.vmId = 42;
        afterAction.status = SysAdminAction.Status.IN_PROGRESS;
        afterAction.sysAdminActionId = 12;
        afterAction.createdAt = ZonedDateTime.now(ZoneId.of("UTC")).format(TimeStamp.hfsActionTimestampFormat);

        when(sysAdminService.getSysAdminAction(1)).thenReturn(beforeAction);
        when(sysAdminService.usageStatsUpdate(42, 1)).thenReturn(afterAction);

        VmUsage usage = vmUsageService.getUsage(42);

        verify(sysAdminService).usageStatsUpdate(42, 1);
    }
}
