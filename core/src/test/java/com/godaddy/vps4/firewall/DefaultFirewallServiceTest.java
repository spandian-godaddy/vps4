package com.godaddy.vps4.firewall;

import com.godaddy.vps4.firewall.model.FirewallDetail;
import com.godaddy.vps4.firewall.model.FirewallSite;
import com.godaddy.vps4.firewall.model.FirewallStatus;
import com.godaddy.vps4.firewall.model.VmFirewallSite;
import com.godaddy.vps4.firewall.model.FirewallBypassWAF;
import com.godaddy.vps4.firewall.model.FirewallClientUpdateRequest;
import com.godaddy.vps4.firewall.model.FirewallCacheLevel;

import com.godaddy.vps4.sso.Vps4SsoService;
import com.godaddy.vps4.sso.models.Vps4SsoToken;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.runners.MockitoJUnitRunner;

import javax.ws.rs.NotFoundException;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.anyString;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.eq;

@RunWith(MockitoJUnitRunner.class)
public class DefaultFirewallServiceTest {
    private FirewallClientService firewallClientService = mock(FirewallClientService.class);
    private FirewallDataService firewallDataService = mock(FirewallDataService.class);
    private Vps4SsoService vps4SsoService = mock(Vps4SsoService.class);
    private FirewallService service;
    @Captor
    private ArgumentCaptor<FirewallClientUpdateRequest> modifyFirewallArgumentCaptor;

    String shopperId = "12345678";
    String shopperJwt = "fakeShopperJwt";
    UUID vmId = UUID.randomUUID();
    Vps4SsoToken token;
    FirewallSite firewallSite;
    VmFirewallSite vmFirewallSite;
    FirewallDetail firewallSiteDetail;

    @Before
    public void setupTest() {
        service = new DefaultFirewallService(firewallClientService, firewallDataService, vps4SsoService);

        firewallSite = new FirewallSite();
        firewallSite.siteId = "fakeSiteId";
        firewallSite.domain = "fakeDomain.com";
        firewallSite.anyCastIP = "0.0.0.0";
        firewallSite.status = FirewallStatus.SUCCESS;
        firewallSite.planId = "fakePlanId";

        firewallSiteDetail = new FirewallDetail();
        firewallSiteDetail.siteId = "fakeSiteDetailId";

        vmFirewallSite = new VmFirewallSite();
        vmFirewallSite.siteId = firewallSite.siteId;

        token = new Vps4SsoToken(1,"fakeMessage", shopperJwt);
        when(vps4SsoService.getDelegationToken("idp", shopperId)).thenReturn(token);
        when(firewallClientService.getFirewallSites(anyString())).thenReturn(Collections.singletonList(firewallSite));
        when(firewallClientService.getFirewallSiteDetail(anyString(), anyString())).thenReturn(firewallSiteDetail);

        when(firewallDataService.getFirewallSiteFromId(eq(vmId), anyString())).thenReturn(vmFirewallSite);
        when(firewallDataService.getActiveFirewallSitesOfVm(eq(vmId))).thenReturn(Collections.singletonList(vmFirewallSite));
    }

    @Test
    public void testGetFirewallSitesNullCustomerJwt() {
        List<FirewallSite> response = service.getFirewallSites(shopperId, null, vmId);
        verify(vps4SsoService, times(1)).getDelegationToken("idp", shopperId);
        verify(firewallClientService, times(1)).getFirewallSites("sso-jwt " + shopperJwt);

        assertEquals(1, response.size());
        assertSame(firewallSite, response.get(0));
    }

    @Test
    public void testGetFirewallSitesWithCustomerJwt() {
        List<FirewallSite> response = service.getFirewallSites(shopperId, "customerJwt", vmId);
        verify(vps4SsoService, times(0)).getDelegationToken("idp", shopperId);
        verify(firewallClientService, times(1)).getFirewallSites("sso-jwt customerJwt");

        assertEquals(1, response.size());
        assertSame(firewallSite, response.get(0));
    }

    @Test
    public void testGetFirewallSitesDbEmptyList() {
        when(firewallDataService.getActiveFirewallSitesOfVm(eq(vmId))).thenReturn(Collections.emptyList());

        List<FirewallSite> response = service.getFirewallSites(shopperId, "customerJwt", vmId);
        verify(vps4SsoService, times(0)).getDelegationToken("idp", shopperId);
        verify(firewallClientService, times(1)).getFirewallSites("sso-jwt customerJwt");

        assertEquals(0, response.size());
    }

    @Test
    public void testGetFirewallSitesFromListNoMatch() {
        VmFirewallSite wrongFirewallSite = new VmFirewallSite();
        wrongFirewallSite.siteId = "wrongSiteId";
        when(firewallDataService.getActiveFirewallSitesOfVm(eq(vmId))).thenReturn(Collections.singletonList(wrongFirewallSite));
        List<FirewallSite> response = service.getFirewallSites(shopperId, "customerJwt", vmId);
        verify(vps4SsoService, times(0)).getDelegationToken("idp", shopperId);
        verify(firewallClientService, times(1)).getFirewallSites("sso-jwt customerJwt");

        assertEquals(0, response.size());
    }

    @Test
    public void testGetFirewallSiteNullCustomerJwt() {
        FirewallDetail response = service.getFirewallSiteDetail(shopperId, null, firewallSiteDetail.siteId, vmId);
        verify(vps4SsoService, times(1)).getDelegationToken("idp", shopperId);
        verify(firewallClientService, times(1)).getFirewallSiteDetail("sso-jwt " + shopperJwt, firewallSiteDetail.siteId);

        assertSame(firewallSiteDetail, response);
    }

    @Test
    public void testGetFirewallSiteWithCustomerJwt() {
        FirewallDetail response = service.getFirewallSiteDetail(shopperId, "customerJwt", firewallSiteDetail.siteId, vmId);
        verify(vps4SsoService, times(0)).getDelegationToken("idp", shopperId);
        verify(firewallClientService, times(1)).getFirewallSiteDetail("sso-jwt customerJwt", firewallSiteDetail.siteId);

        assertSame(firewallSiteDetail, response);
    }

    @Test(expected = NotFoundException.class)
    public void testGetFirewallSiteDetailDbNotFoundThrowsException() {
        when(firewallDataService.getFirewallSiteFromId(eq(vmId), anyString())).thenReturn(null);

        service.getFirewallSiteDetail(shopperId, "customerJwt", firewallSiteDetail.siteId, vmId);
    }

    @Test
    public void testDeleteFirewallSiteSuccessful() {
        service.deleteFirewallSite(shopperId, null, firewallSiteDetail.siteId);
        verify(vps4SsoService, times(1)).getDelegationToken("idp", shopperId);
        verify(firewallClientService, times(1)).deleteFirewallSite("sso-jwt " + shopperJwt, firewallSiteDetail.siteId);
    }

    @Test(expected = NotFoundException.class)
    public void testDeleteFirewallSiteThrowsException() {
        when(firewallClientService.deleteFirewallSite(anyString(), anyString())).thenThrow(new NotFoundException());

        service.deleteFirewallSite(shopperId, null, firewallSiteDetail.siteId);
    }

    @Test
    public void testModifyFirewallSiteSuccessful() {
        service.updateFirewallSite(shopperId, null, firewallSiteDetail.siteId, FirewallCacheLevel.CACHING_DISABLED, FirewallBypassWAF.DISABLED);

        verify(vps4SsoService, times(1)).getDelegationToken("idp", shopperId);
        verify(firewallClientService, times(1))
                .modifyFirewallSite(eq("sso-jwt " + shopperJwt), eq(firewallSiteDetail.siteId), modifyFirewallArgumentCaptor.capture());

        FirewallClientUpdateRequest req = modifyFirewallArgumentCaptor.getValue();

        assertEquals(FirewallBypassWAF.DISABLED.toString(), req.bypassWAF);
        assertEquals(FirewallCacheLevel.CACHING_DISABLED.toString(), req.cacheLevel);
    }

    @Test
    public void testModifyFirewallSiteNullBypassWAF() {
        service.updateFirewallSite(shopperId, null, firewallSiteDetail.siteId, FirewallCacheLevel.CACHING_DISABLED, null);
        verify(firewallClientService, times(1))
                .modifyFirewallSite(eq("sso-jwt " + shopperJwt), eq(firewallSiteDetail.siteId), modifyFirewallArgumentCaptor.capture());

        FirewallClientUpdateRequest req = modifyFirewallArgumentCaptor.getValue();

        assertEquals(null, req.bypassWAF);
        assertEquals(FirewallCacheLevel.CACHING_DISABLED.toString(), req.cacheLevel);
    }

    @Test
    public void testModifyFirewallSiteNullCacheLevel() {
        service.updateFirewallSite(shopperId, null, firewallSiteDetail.siteId, null, FirewallBypassWAF.DISABLED);
        verify(firewallClientService, times(1))
                .modifyFirewallSite(eq("sso-jwt " + shopperJwt), eq(firewallSiteDetail.siteId), modifyFirewallArgumentCaptor.capture());

        FirewallClientUpdateRequest req = modifyFirewallArgumentCaptor.getValue();

        assertEquals(FirewallBypassWAF.DISABLED.toString(), req.bypassWAF);
        assertEquals(null, req.cacheLevel);
    }


    @Test(expected = NotFoundException.class)
    public void testModifyFirewallSiteThrowsException() {
        when(firewallClientService.modifyFirewallSite(anyString(), anyString(), any())).thenThrow(new NotFoundException());

        service.updateFirewallSite(shopperId, null, firewallSiteDetail.siteId, null, FirewallBypassWAF.DISABLED);
    }
}
