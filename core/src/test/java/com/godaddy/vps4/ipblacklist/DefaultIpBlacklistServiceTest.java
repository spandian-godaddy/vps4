package com.godaddy.vps4.ipblacklist;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.json.simple.JSONObject;
import org.junit.Before;
import org.junit.Test;

import com.godaddy.hfs.config.Config;

public class DefaultIpBlacklistServiceTest {

    private Config config = mock(Config.class);
    private IpBlacklistClientService blacklistClientService = mock(IpBlacklistClientService.class);
    private IpBlacklistService service;

    @Before
    public void setupTest() {
        service = new DefaultIpBlacklistService(config, blacklistClientService);
    }

    @Test
    public void testIsIpBlacklistedTrue() {
        String ip = "192.168.0.1";
        JSONObject result = new JSONObject();
        result.put("data", "test data");
        when(blacklistClientService.getBlacklistRecord(ip)).thenReturn(result);
        boolean isBlacklisted = service.isIpBlacklisted(ip);
        assertTrue(isBlacklisted);
    }

    @Test
    public void testIsIpBlacklistedFalse() {
        String ip = "192.168.0.1";
        JSONObject result = new JSONObject();
        result.put("badData", "test data");
        when(blacklistClientService.getBlacklistRecord(ip)).thenReturn(result);
        boolean isBlacklisted = service.isIpBlacklisted(ip);
        assertFalse(isBlacklisted);
    }

    @Test
    public void testBlacklistIp() {
        String ip = "192.168.0.1";
        service.blacklistIp(ip);
        verify(blacklistClientService, times(1)).createBlacklistRecord(ip);
    }

    @Test
    public void testRemoveIpFromBlacklist() {
        String ip = "192.168.0.1";
        service.removeIpFromBlacklist(ip);
        verify(blacklistClientService, times(1)).deleteBlacklistRecord(ip);
    }
}
