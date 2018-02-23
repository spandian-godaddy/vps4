package com.godaddy.vps4.mailrelay;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

import javax.cache.Cache;
import javax.cache.CacheManager;

import com.godaddy.vps4.cache.CacheName;
import com.godaddy.vps4.mailrelay.MailRelayService.CachedMailRelayHistory;

import org.junit.Before;
import org.junit.Test;

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
        Assert.assertEquals(0, result.size());
    }

    private void testGetCachedMailRelayWithDaysLimit(int days){
        List<MailRelayHistory> history = new ArrayList<MailRelayHistory>();
        MailRelayHistory twoDaysAgoHistory = new MailRelayHistory();
        twoDaysAgoHistory.date = LocalDate.now().minus(2, ChronoUnit.DAYS).toString();
        MailRelayHistory yesterdayHisotry = new MailRelayHistory();
        yesterdayHisotry.date =  LocalDate.now().minus(1, ChronoUnit.DAYS).toString();
        history.add(twoDaysAgoHistory);
        history.add(yesterdayHisotry);
        CachedMailRelayHistory cachedHistory = new CachedMailRelayHistory();
        cachedHistory.relayHistory = history;
        when(mailRelayHistoryCache.get(this.ipAddress)).thenReturn(cachedHistory);
        LocalDate startDate = LocalDate.parse(twoDaysAgoHistory.date);
        List<MailRelayHistory> result = relayService.getMailRelayHistory(this.ipAddress, startDate, days);
        verify(hfsRelayService, times(0)).getRelayHistory(this.ipAddress);
        Assert.assertEquals(days, result.size());
    }

    @Test
    public void testGetCachedMailRelayMoreRelaysThanLimit(){
        testGetCachedMailRelayWithDaysLimit(1);
    }

    @Test
    public void testGetCachedMailRelaySameDaysAsLimit(){
        testGetCachedMailRelayWithDaysLimit(2);
    }

    @Test
    public void testGetCachedMailRelayHigherLimitThanDays(){
        List<MailRelayHistory> history = new ArrayList<MailRelayHistory>();
        MailRelayHistory oldHistory = new MailRelayHistory();
        oldHistory.date = LocalDate.now().minus(2, ChronoUnit.DAYS).toString();
        MailRelayHistory newHistory = new MailRelayHistory();
        newHistory.date = LocalDate.now().minus(1, ChronoUnit.DAYS).toString();
        history.add(oldHistory);
        history.add(newHistory);
        CachedMailRelayHistory cachedHistory = new CachedMailRelayHistory();
        cachedHistory.relayHistory = history;
        when(mailRelayHistoryCache.get(this.ipAddress)).thenReturn(cachedHistory);
        LocalDate startDate = LocalDate.parse(oldHistory.date);
        List<MailRelayHistory> result = relayService.getMailRelayHistory(this.ipAddress, startDate, 3);
        verify(hfsRelayService, times(0)).getRelayHistory(this.ipAddress);
        Assert.assertEquals(2, result.size());
    }
}
