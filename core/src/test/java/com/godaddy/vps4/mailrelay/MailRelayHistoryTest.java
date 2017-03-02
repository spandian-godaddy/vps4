package com.godaddy.vps4.mailrelay;

import javax.cache.Cache;
import javax.cache.CacheManager;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;

import static org.mockito.Mockito.verify;

import org.junit.Before;
import org.junit.Test;

import com.godaddy.vps4.cache.CacheName;
import com.godaddy.vps4.mailrelay.MailRelayService.CachedMailRelayHistory;

import gdg.hfs.vhfs.mailrelay.MailRelayHistory;
import junit.framework.Assert;

public class MailRelayHistoryTest {
    
    gdg.hfs.vhfs.mailrelay.MailRelayService hfsRelayService;
    Cache<String, CachedMailRelayHistory> mailRelayHistoryCache;
    MailRelayService relayService;
    List<MailRelayHistory> relayHistory;
    String ipAddress = "1.2.3.4";
    
    @Before
    public void setUp(){
        relayHistory = new ArrayList<>();
        hfsRelayService = mock(gdg.hfs.vhfs.mailrelay.MailRelayService.class);
        when(hfsRelayService.getRelayHistory(this.ipAddress)).thenReturn (relayHistory);
        CacheManager cacheManager = mock(CacheManager.class);
        mailRelayHistoryCache = mock(Cache.class);
        when(cacheManager.getCache(CacheName.MAIL_RELAY_HISTORY, String.class, CachedMailRelayHistory.class)).thenReturn(mailRelayHistoryCache);
        relayService = new MailRelayService(hfsRelayService, cacheManager);
        
    }
    
    @Test
    public void testGetMailRelay(){
        // works as normal when cache is empty
        when(mailRelayHistoryCache.get(this.ipAddress)).thenReturn(null);
        List<MailRelayHistory> result = relayService.getMailRelayHistory(this.ipAddress);
        verify(hfsRelayService, times(1)).getRelayHistory(this.ipAddress);
        Assert.assertEquals(this.relayHistory, result);
    }
    
    @Test
    public void testGetCachedMailRelay(){
        // returns cached result when one is available
        CachedMailRelayHistory cachedHistory = new CachedMailRelayHistory();
        when(mailRelayHistoryCache.get(this.ipAddress)).thenReturn(cachedHistory);
        List<MailRelayHistory> result = relayService.getMailRelayHistory(this.ipAddress);
        verify(hfsRelayService, times(0)).getRelayHistory(this.ipAddress);
        Assert.assertEquals(cachedHistory.relayHistory, result);
    }
}
