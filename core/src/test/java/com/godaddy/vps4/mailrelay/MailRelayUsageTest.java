package com.godaddy.vps4.mailrelay;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import javax.cache.Cache;
import javax.cache.CacheManager;

import org.junit.Before;
import org.junit.Test;

import com.godaddy.vps4.mailrelay.MailRelayService.CachedMailRelayUsage;

import com.godaddy.hfs.mailrelay.MailRelay;
import junit.framework.Assert;

public class MailRelayUsageTest {

    com.godaddy.hfs.mailrelay.MailRelayService hfsRelayService;
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
        hfsRelayService = mock(com.godaddy.hfs.mailrelay.MailRelayService.class);
        when(hfsRelayService.getMailRelay(this.ipAddress)).thenReturn (mailRelay);
        CacheManager cacheManager = mock(CacheManager.class);
        when(hfsRelayService.getMailRelay(ipAddress)).thenReturn(mailRelay);
        relayService = new MailRelayService(hfsRelayService, cacheManager);

    }

    @Test
    public void testGetMailRelay(){
        // returns the result of the hfs mail relay.
        MailRelay result = relayService.getMailRelay(this.ipAddress);
        verify(hfsRelayService, times(1)).getMailRelay(this.ipAddress);
        Assert.assertEquals(mailRelay, result);
    }
}
