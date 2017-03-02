package com.godaddy.vps4.mailrelay;

import javax.cache.Cache;
import javax.cache.CacheManager;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verify;

import org.junit.Before;
import org.junit.Test;

import com.godaddy.vps4.cache.CacheName;
import com.godaddy.vps4.mailrelay.MailRelayService.CachedMailRelayUsage;

import gdg.hfs.vhfs.mailrelay.MailRelay;
import junit.framework.Assert;

public class MailRelayUsageTest {
    
    gdg.hfs.vhfs.mailrelay.MailRelayService hfsRelayService;
    Cache<String, CachedMailRelayUsage> mailRelayCache;
    MailRelayService relayService;
    MailRelay mailRelay;
    String ipAddress = "1.2.3.4";
    
    @Before
    public void setUp(){
        mailRelay = new MailRelay();
        mailRelay.ipv4Address = this.ipAddress;
        mailRelay.quota = 5000;
        mailRelay.relays = 100;
        hfsRelayService = mock(gdg.hfs.vhfs.mailrelay.MailRelayService.class);
        when(hfsRelayService.getMailRelay(this.ipAddress)).thenReturn (mailRelay);
        CacheManager cacheManager = mock(CacheManager.class);
        mailRelayCache = mock(Cache.class);
        when(cacheManager.getCache(CacheName.MAIL_RELAY_USAGE, String.class, CachedMailRelayUsage.class)).thenReturn(mailRelayCache);
        relayService = new MailRelayService(hfsRelayService, cacheManager);
        
    }
    
    @Test
    public void testGetMailRelay(){
        // works as normal when cache is empty
        when(mailRelayCache.get(this.ipAddress)).thenReturn(null);
        MailRelay result = relayService.getMailRelay(this.ipAddress);
        verify(hfsRelayService, times(1)).getMailRelay(this.ipAddress);
        Assert.assertEquals(this.mailRelay, result);
    }
    
    @Test
    public void testGetCachedMailRelay(){
        // returns cached result when one is available
        MailRelay newMailRelay = new MailRelay();
        mailRelay.ipv4Address = this.ipAddress;
        mailRelay.quota = 5000;
        mailRelay.relays = 1000;
        CachedMailRelayUsage cachedUsage = new CachedMailRelayUsage(newMailRelay);
        when(mailRelayCache.get(this.ipAddress)).thenReturn(cachedUsage);
        MailRelay result = relayService.getMailRelay(this.ipAddress);
        verify(hfsRelayService, times(0)).getMailRelay(this.ipAddress);
        Assert.assertEquals(newMailRelay, result);
    }
}
