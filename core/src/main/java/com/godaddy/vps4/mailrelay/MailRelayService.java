package com.godaddy.vps4.mailrelay;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import javax.cache.Cache;
import javax.cache.CacheManager;
import javax.inject.Inject;

import com.godaddy.hfs.mailrelay.MailRelay;
import com.godaddy.hfs.mailrelay.MailRelayHistory;
import com.godaddy.vps4.cache.CacheName;

public class MailRelayService {

    final com.godaddy.hfs.mailrelay.MailRelayService relayService;
    private final Cache<String, CachedMailRelayHistory> mailRelayHistoryCache;

    @Inject
    public  MailRelayService(com.godaddy.hfs.mailrelay.MailRelayService relayService, CacheManager cacheManager){
        this.relayService = relayService;
        this.mailRelayHistoryCache = cacheManager.getCache(CacheName.MAIL_RELAY_HISTORY, String.class, CachedMailRelayHistory.class);
    }

    public MailRelay getMailRelay(String ipAddress){
        return relayService.getMailRelay(ipAddress);
    }

    public void resetMailRelayHistoryCache(String ipAddress){
        mailRelayHistoryCache.remove(ipAddress);
    }

    public List<MailRelayHistory> getMailRelayHistory(String ipAddress){
        // At the time of writing this code, HFS returns a maximum of 90 days of data.
        LocalDate startDate = LocalDate.now().minus(90, ChronoUnit.DAYS);
        return getMailRelayHistory(ipAddress, startDate, 90);
    }

    public List<MailRelayHistory> getMailRelayHistory(String ipAddress, LocalDate startDate){
        // At the time of writing this code, HFS returns a maximum of 90 days of data.
        return getMailRelayHistory(ipAddress, startDate, 90);
    }

    public List<MailRelayHistory> getMailRelayHistory(String ipAddress, LocalDate startDate, int daysToReturn){
        LocalDate today = LocalDate.now();
        LocalDate daysAgo = today.minus(daysToReturn, ChronoUnit.DAYS);

        CachedMailRelayHistory cachedHistory = mailRelayHistoryCache.get(ipAddress);
        if (cachedHistory == null){
            List<MailRelayHistory> newHistory = relayService.getRelayHistory(ipAddress);
            cachedHistory = new CachedMailRelayHistory(newHistory);
            mailRelayHistoryCache.put(ipAddress, cachedHistory);
        }
        if (cachedHistory.relayHistory == null){
            return new ArrayList<MailRelayHistory>();
        }
        return cachedHistory.relayHistory.stream().
                filter(quota -> (startDate.compareTo(LocalDate.parse(quota.date)) <= 0) &&
                        (daysAgo.compareTo(LocalDate.parse(quota.date)) <= 0)).
                collect(Collectors.toList());

    }

    public static class CachedMailRelayHistory {
            public List<MailRelayHistory> relayHistory;
            public CachedMailRelayHistory(){
            }
            public CachedMailRelayHistory(List<MailRelayHistory> relayHistory){
                this.relayHistory = relayHistory;
            }
        }
}
