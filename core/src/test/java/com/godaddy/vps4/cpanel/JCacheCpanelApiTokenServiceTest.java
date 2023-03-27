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

public class JCacheCpanelApiTokenServiceTest {

    private CpanelApiTokenService mockCpanelApiTokenService;
    private JCacheCpanelApiTokenService spyApiTokenService;
    private Cache<Long, String> mockCache;

    @Before
    public void setupTest() {
        mockCpanelApiTokenService = Mockito.mock(CpanelApiTokenService.class);
        CacheManager mockCacheManager = Mockito.mock(CacheManager.class);
        mockCache = (Cache<Long, String>)Mockito.mock(Cache.class);
        Mockito.when(mockCacheManager.getCache("cpanel.api.token", Long.class, String.class))
                .thenReturn(mockCache);
        JCacheCpanelApiTokenService apiTokenService = new JCacheCpanelApiTokenService(mockCpanelApiTokenService,
                mockCacheManager);
        spyApiTokenService = Mockito.spy(apiTokenService);
    }

    @Test
    public void testInvalidApiToken() {
        Random random = new Random();
        long vmId = random.nextLong();
        String apiToken = UUID.randomUUID().toString();

        spyApiTokenService.invalidateApiToken(vmId, apiToken);
        Mockito.verify(spyApiTokenService.cache, Mockito.times(1)).remove(vmId);
    }

    @Test
    public void testGetApiToken() {
        Random random = new Random();
        long vmId = random.nextLong();
        int epochSeconds = random.nextInt();
        Instant timeoutAt = Instant.ofEpochSecond(epochSeconds);
        String apiToken = UUID.randomUUID().toString();
        Mockito.when(mockCache.get(vmId)).thenReturn(apiToken);

        Assert.assertEquals(apiToken, spyApiTokenService.getApiToken(vmId, timeoutAt));
        Mockito.verify(mockCache, Mockito.times(1)).get(vmId);
    }

    @Test
    public void testGetNestedApiTokenService() {
        Random random = new Random();
        long vmId = random.nextLong();
        int epochSeconds = random.nextInt();
        Instant timeoutAt = Instant.ofEpochSecond(epochSeconds);

        Mockito.when(mockCache.get(vmId)).thenReturn(null);
        Mockito.when(mockCpanelApiTokenService.getApiToken(vmId, timeoutAt))
                .thenReturn(null);

        Assert.assertEquals(null, spyApiTokenService.getApiToken(vmId, timeoutAt));
        Mockito.verify(mockCpanelApiTokenService, Mockito.times(1))
                .getApiToken(vmId, timeoutAt);
    }
}
