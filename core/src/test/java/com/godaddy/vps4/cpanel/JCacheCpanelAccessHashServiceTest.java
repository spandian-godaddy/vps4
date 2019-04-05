package com.godaddy.vps4.cpanel;

import java.time.Instant;
import java.util.Random;
import java.util.UUID;

import javax.cache.Cache;
import javax.cache.CacheManager;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

public class JCacheCpanelAccessHashServiceTest {

    private CpanelAccessHashService mockCpanelAccessHashService;
    private JCacheCpanelAccessHashService spyHashService;
    private Cache<Long, String> mockCache;

    @Before
    public void setupTest() {
        mockCpanelAccessHashService = Mockito.mock(CpanelAccessHashService.class);
        CacheManager mockCacheManager = Mockito.mock(CacheManager.class);
        mockCache = (Cache<Long, String>)Mockito.mock(Cache.class);
        Mockito.when(mockCacheManager.getCache("cpanel.accesshash", Long.class, String.class))
                .thenReturn(mockCache);
        JCacheCpanelAccessHashService hashService = new JCacheCpanelAccessHashService(mockCpanelAccessHashService,
                mockCacheManager);
        spyHashService = Mockito.spy(hashService);
    }

    @Test
    public void testInvalidAccessHash() {
        Random random = new Random();
        long vmId = random.nextLong();
        String accessHash = UUID.randomUUID().toString();

        spyHashService.invalidAccessHash(vmId, accessHash);
        Mockito.verify(spyHashService.cache, Mockito.times(1)).remove(vmId);
    }

    @Test
    public void testGetAccessHash() {
        Random random = new Random();
        long vmId = random.nextLong();
        String publicIp = UUID.randomUUID().toString();
        String fromIp = UUID.randomUUID().toString();
        int epochSeconds = random.nextInt();
        Instant timeoutAt = Instant.ofEpochSecond(epochSeconds);
        String accessHash = UUID.randomUUID().toString();
        Mockito.when(mockCache.get(vmId)).thenReturn(accessHash);

        Assert.assertEquals(accessHash, spyHashService.getAccessHash(vmId, publicIp, timeoutAt));
        Mockito.verify(mockCache, Mockito.times(1)).get(vmId);
    }

    @Test
    public void testGetNestedAccessHashService() {
        Random random = new Random();
        long vmId = random.nextLong();
        String publicIp = UUID.randomUUID().toString();
        String fromIp = UUID.randomUUID().toString();
        int epochSeconds = random.nextInt();
        Instant timeoutAt = Instant.ofEpochSecond(epochSeconds);
        String accessHash = null;
        Mockito.when(mockCache.get(vmId)).thenReturn(accessHash);
        Mockito.when(mockCpanelAccessHashService.getAccessHash(vmId, publicIp, timeoutAt))
                .thenReturn(accessHash);

        Assert.assertEquals(accessHash, spyHashService.getAccessHash(vmId, publicIp, timeoutAt));
        Mockito.verify(mockCpanelAccessHashService, Mockito.times(1))
                .getAccessHash(vmId, publicIp, timeoutAt);
    }
}
