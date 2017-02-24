package com.godaddy.vps4.plesk;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.godaddy.hfs.config.Config;
import com.godaddy.vps4.util.PollerTimedOutException;
import com.google.inject.Injector;

import gdg.hfs.vhfs.plesk.PleskAction;
import gdg.hfs.vhfs.plesk.PleskAction.Status;
import gdg.hfs.vhfs.plesk.PleskService;

public class Vps4PleskServiceTest {
    
    PleskService pleskService;
    Vps4PleskActionPoller poller;
    PleskAction pleskAction;
    Config config;
    Injector injector;
    final int timeoutValue = 1000;
    final int hfsVmId = 6360;
    
    Vps4PleskService vps4PleskService;  
    
    @Before
    public void setUp() throws Exception {
        
        pleskService = mock(PleskService.class);
        poller = mock(Vps4PleskActionPoller.class);
        config = new Config() {
            
            @Override
            public byte[] getData(String path) {
                return null;
            }
            
            @Override
            public String get(String key) {
                return null;
            }
            
            @Override
            public String get(String key, String defaultValue) {
                if(key == "vps4.callable.timeout") {
                    return "1000";
                }
                return null;
            }
        };
        
        vps4PleskService = new DefaultVps4PleskService(pleskService, config, poller);
        
        //setup a fake plesk action;
        pleskAction = new PleskAction();
        pleskAction.actionId = 66;
        pleskAction.serverId = 6360;
        pleskAction.vmId = 6360;
        pleskAction.status = Status.COMPLETE;
        pleskAction.cncRequestId = 46538;
        pleskAction.actionType = 1;
        pleskAction.createdAt = "On-the-day-of-Easter";
        pleskAction.modifiedAt = "On-the-day-of-Pesach";
        pleskAction.completedAt = "On-the-day-of-Gudhi-Padwa";
    }

    @After
    public void tearDown() throws Exception {
        pleskService = null;
        poller = null;
        pleskAction = null;
    }

    @Test
    public void testListPleskAccounts() throws PollerTimedOutException, Exception {
        //String responsePayload = "{\"sites\":\"{'name': 'atulscreativedomain.com','webspace': 'Physical hosting','ip_address': '10.198.32.140','ftp_login': 'pleskvmAdmin','diskused': '0 B',},{'name': 'atulsmightydomain.com','webspace': 'Physical hosting','ip_address': '10.198.32.140','ftp_login': 'pleskvmAdmin','diskused': '0 B',},{'name': 'atulsPleskVM07.secureserver.net','webspace': 'Physical hosting','ip_address': '10.198.32.140','ftp_login': 'pleskvmAdmin','diskused': '3.66 MB',},{'name': 'atulsuperdomain1.com','webspace': 'Physical hosting','ip_address': '10.198.32.140','ftp_login': 'pleskvmAdmin','diskused': '0 B',},{'name': 'george.omg','webspace': 'Physical hosting','ip_address': '10.198.32.140','ftp_login': 'pleskvmAdmin','diskused': '0 B',},{'name': 'newtestsubscriptiondomain.com','webspace': 'Physical hosting','ip_address': '10.198.32.140','ftp_login': 'someUser123','diskused': '298 KB',}\"}";
        String responsePayload = "{ \"sites\": [ { \"name\": \"atulscreativedomain.com\", \"webspace\": \"Physical hosting\", \"ip_address\": \"10.198.32.140\", \"ftp_login\": \"pleskvmAdmin\", \"diskused\": \"0 B\", }, { \"name\": \"atulsmightydomain.com\", \"webspace\": \"Physical hosting\", \"ip_address\": \"10.198.32.140\", \"ftp_login\": \"pleskvmAdmin\", \"diskused\": \"0 B\", }, { \"name\": \"atulsPleskVM07.secureserver.net\", \"webspace\": \"Physical hosting\", \"ip_address\": \"10.198.32.140\", \"ftp_login\": \"pleskvmAdmin\", \"diskused\": \"3.66 MB\", }, { \"name\": \"atulsuperdomain1.com\", \"webspace\": \"Physical hosting\", \"ip_address\": \"10.198.32.140\", \"ftp_login\": \"pleskvmAdmin\", \"diskused\": \"0 B\", }, { \"name\": \"george.omg\", \"webspace\": \"Physical hosting\", \"ip_address\": \"10.198.32.140\", \"ftp_login\": \"pleskvmAdmin\", \"diskused\": \"0 B\", }, { \"name\": \"newtestsubscriptiondomain.com\", \"webspace\": \"Physical hosting\", \"ip_address\": \"10.198.32.140\", \"ftp_login\": \"someUser123\", \"diskused\": \"298 KB\", } ] }";

        pleskAction.responsePayload = responsePayload;
        
        when(pleskService.requestSiteList(hfsVmId)).thenReturn(pleskAction);
        when(poller.poll(pleskAction, timeoutValue)).thenReturn(responsePayload);
        
        try {
            List<PleskAccount> pleskAccounts = vps4PleskService.listPleskAccounts(hfsVmId);
            assertNotNull("Plesk accounts list should not be empty.", pleskAccounts);
            assertTrue("Plesk accounts list length should be greater than 0.", pleskAccounts.size() > 0);
        }
        catch (Exception e) {
            fail("Encountered unexpected exception in listPleskAccounts, failing test. Exception: " + e);
        }
    }

    @Test
    public void testGetPleskSsoUrl() throws PollerTimedOutException, Exception {

        String fromIpAddress = "my.super.fake.ip.address";
        String expectedSsoUrl = "https://192.169.148.52:8443/enterprise/rsession_init.php?PLESKSESSID=5fe2a93e3b182912b899be1213d50c04";
        String responsePayload = "{\"ssoUrl\":\"https://192.169.148.52:8443/enterprise/rsession_init.php?PLESKSESSID=5fe2a93e3b182912b899be1213d50c04\"}";
        pleskAction.responsePayload = responsePayload;
        
        when(pleskService.requestAccess(hfsVmId, fromIpAddress)).thenReturn(pleskAction);
        when(poller.poll(pleskAction, timeoutValue)).thenReturn(responsePayload);
        
        try {
            PleskSession session = vps4PleskService.getPleskSsoUrl(hfsVmId, fromIpAddress);
            assertNotNull("Plesk accounts list should not be empty.", session.getSsoUrl());
            assertEquals("Expected SSO URL does not match actual SSO URL.", expectedSsoUrl, session.getSsoUrl());
        }
        catch (Exception e) {
            fail("Encountered unexpected exception in getPleskSsoUrl, failing test. Exception: " + e);
        }
    }

}
