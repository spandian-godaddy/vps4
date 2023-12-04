package com.godaddy.vps4.firewall;

import com.godaddy.hfs.config.Config;
import com.godaddy.vps4.firewall.model.FirewallDetail;
import com.godaddy.vps4.firewall.model.FirewallSite;
import com.godaddy.vps4.firewall.model.FirewallStatus;
import com.godaddy.vps4.ipblacklist.DefaultIpBlacklistService;
import com.godaddy.vps4.ipblacklist.IpBlacklistClientService;
import com.godaddy.vps4.ipblacklist.IpBlacklistService;
import com.godaddy.vps4.sso.Vps4SsoService;
import com.godaddy.vps4.sso.models.Vps4SsoToken;
import org.json.simple.JSONObject;
import org.junit.Before;
import org.junit.Test;

import javax.ws.rs.NotFoundException;

import java.util.Collections;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

public class DefaultFirewallServiceTest {
    private FirewallClientService firewallClientService = mock(FirewallClientService.class);
    private Vps4SsoService vps4SsoService = mock(Vps4SsoService.class);
    private FirewallService service;

    String shopperId = "12345678";
    String shopperJwt = "fakeShopperJwt";
    Vps4SsoToken token;
    FirewallSite firewallSite;
    FirewallDetail firewallSiteDetail;

    @Before
    public void setupTest() {
        service = new DefaultFirewallService(firewallClientService, vps4SsoService);

        firewallSite = new FirewallSite();
        firewallSite.siteId = "fakeSiteId";
        firewallSite.domain = "fakeDomain.com";
        firewallSite.anyCastIP = "0.0.0.0";
        firewallSite.status = FirewallStatus.SUCCESS;
        firewallSite.planId = "fakePlanId";

        firewallSiteDetail = new FirewallDetail();
        firewallSiteDetail.siteId = "fakeSiteDetailId";

        token = new Vps4SsoToken(1,"fakeMessage", shopperJwt);
        when(vps4SsoService.getDelegationToken("idp", shopperId)).thenReturn(token);
        when(firewallClientService.getFirewallSites(anyString())).thenReturn(new FirewallSite[]{firewallSite});
        when(firewallClientService.getFirewallSiteDetail(anyString(), anyString())).thenReturn(firewallSiteDetail);
    }

    @Test
    public void testGetAllFirewallSitesNullCustomerJwt() {
        FirewallSite[] firewallSites = service.getAllFirewallSites(shopperId, null);
        verify(vps4SsoService, times(1)).getDelegationToken("idp", shopperId);
        verify(firewallClientService, times(1)).getFirewallSites("sso-jwt " + shopperJwt);

        assertEquals(1, firewallSites.length);
        assertSame(firewallSite, firewallSites[0]);
    }

    @Test
    public void testGetAllFirewallSitesWithCustomerJwt() {
        FirewallSite[] firewallSites = service.getAllFirewallSites(shopperId, "customerJwt");
        verify(vps4SsoService, times(0)).getDelegationToken("idp", shopperId);
        verify(firewallClientService, times(1)).getFirewallSites("sso-jwt customerJwt");

        assertEquals(1, firewallSites.length);
        assertSame(firewallSite, firewallSites[0]);
    }

    @Test
    public void testGetFirewallSiteNullCustomerJwt() {
        FirewallDetail response = service.getFirewallSiteDetail(shopperId, null, firewallSiteDetail.siteId);
        verify(vps4SsoService, times(1)).getDelegationToken("idp", shopperId);
        verify(firewallClientService, times(1)).getFirewallSiteDetail("sso-jwt " + shopperJwt, firewallSiteDetail.siteId);

        assertSame(firewallSiteDetail, response);
    }

    @Test
    public void testGetFirewallSiteWithCustomerJwt() {
        FirewallDetail response = service.getFirewallSiteDetail(shopperId, "customerJwt", firewallSiteDetail.siteId);
        verify(vps4SsoService, times(0)).getDelegationToken("idp", shopperId);
        verify(firewallClientService, times(1)).getFirewallSiteDetail("sso-jwt customerJwt", firewallSiteDetail.siteId);

        assertSame(firewallSiteDetail, response);
    }
}
