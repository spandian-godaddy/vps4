package com.godaddy.vps4.cpanel;

import com.godaddy.hfs.cpanel.CPanelAction;
import com.godaddy.hfs.cpanel.CPanelService;
import com.godaddy.vps4.cache.CacheName;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import javax.cache.Cache;
import javax.cache.CacheManager;
import java.time.Instant;
import java.util.Random;
import java.util.UUID;

public class HfsCpanelApiTokenServiceTest {

    private CPanelService cPanelService;
    private HfsCpanelApiTokenService spyApiTokenService;
    private Cache<Long, Long> mockCache;
    private long vmId = 10L;
    private long actionId = 11L;
    private CPanelAction hfsAction;

    @Before
    public void setupTest() {
        hfsAction = new CPanelAction();
        hfsAction.actionId = actionId;
        hfsAction.status = CPanelAction.Status.COMPLETE;
        hfsAction.responsePayload = "{\"api_token\":\"completedToken\"}";
        cPanelService = Mockito.mock(CPanelService.class);
        CacheManager mockCacheManager = Mockito.mock(CacheManager.class);
        mockCache = (Cache<Long, Long>)Mockito.mock(Cache.class);

        Mockito.when(mockCacheManager.getCache(CacheName.CPANEL_API_TOKEN_ACTION, Long.class, Long.class))
                .thenReturn(mockCache);
        Mockito.when(cPanelService.requestApiToken(vmId))
                .thenReturn(hfsAction);
        Mockito.when(cPanelService.getAction(actionId))
                .thenReturn(hfsAction);

        spyApiTokenService = Mockito.spy(new HfsCpanelApiTokenService(cPanelService,
                mockCacheManager));
    }

    @Test
    public void testInvalidateApiToken() {
        Random random = new Random();
        long vmId = random.nextLong();
        String apiToken = UUID.randomUUID().toString();

        spyApiTokenService.invalidateApiToken(vmId, apiToken);
        Mockito.verify(spyApiTokenService.cache, Mockito.times(1)).remove(vmId);
    }

    @Test
    public void testGetApiTokenCachedActionId() {
        Random random = new Random();
        int epochSeconds = random.nextInt();
        Instant timeoutAt = Instant.ofEpochSecond(epochSeconds);
        Mockito.when(mockCache.get(vmId)).thenReturn(actionId);

        String apiToken = spyApiTokenService.getApiToken(vmId, timeoutAt);

        Assert.assertEquals("completedToken", apiToken);
        Mockito.verify(mockCache, Mockito.times(1)).get(vmId);
        Mockito.verify(cPanelService, Mockito.times(0)).requestApiToken(vmId);
        Mockito.verify(cPanelService, Mockito.times(1)).getAction(actionId);
    }

    @Test
    public void testGetApiTokenUncachedActionId() {
        Random random = new Random();
        int epochSeconds = random.nextInt();
        Instant timeoutAt = Instant.ofEpochSecond(epochSeconds);
        Mockito.when(mockCache.get(vmId)).thenReturn(actionId);

        String apiToken = spyApiTokenService.getApiToken(vmId, timeoutAt);

        Assert.assertEquals("completedToken", apiToken);
        Mockito.verify(mockCache, Mockito.times(1)).get(vmId);
        Mockito.verify(cPanelService, Mockito.times(0)).requestApiToken(vmId);
        Mockito.verify(cPanelService, Mockito.times(1)).getAction(actionId);
    }

    @Test
    public void testGetApiTokenActionIdFailed() {
        Random random = new Random();
        int epochSeconds = random.nextInt();
        Instant timeoutAt = Instant.ofEpochSecond(epochSeconds);
        hfsAction.status = CPanelAction.Status.FAILED;

        Mockito.when(mockCache.get(vmId)).thenReturn(null);
        Mockito.when(cPanelService.getAction(actionId))
                .thenReturn(hfsAction);

        try {
            spyApiTokenService.getApiToken(vmId, timeoutAt);
        } catch (RuntimeException e) {
            Mockito.verify(mockCache, Mockito.times(1)).get(vmId);
            Mockito.verify(cPanelService, Mockito.times(1)).requestApiToken(vmId);
            Mockito.verify(mockCache, Mockito.times(1)).remove(vmId, actionId);
        }
    }
}
