package com.godaddy.vps4.shopper;

import com.godaddy.vps4.shopper.model.Shopper;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.runners.MockitoJUnitRunner;

import java.net.UnknownHostException;
import java.util.UUID;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.mockito.Mockito.anyString;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class DefaultShopperServiceTest {
    private ShopperApiService shopperApiService = mock(ShopperApiService.class);
    private ShopperService service;
    UUID godaddyCustomerId = UUID.randomUUID();
    UUID brandCustomerId = UUID.randomUUID();
    String gdShopperId = "912348";
    String brandShopperId = "123456";

    Shopper godaddyShopper = new Shopper(godaddyCustomerId, gdShopperId, null, "1");
    Shopper brandResellerShopper = new Shopper(brandCustomerId, brandShopperId, "123456", "232323");


    @Before
    public void setupTest() {
        service = new DefaultShopperService(shopperApiService);

        when(shopperApiService.getShopperByCustomerId(eq(godaddyCustomerId.toString()),anyString())).thenReturn(godaddyShopper);
        when(shopperApiService.getShopperByCustomerId(eq(brandCustomerId.toString()),anyString())).thenReturn(brandResellerShopper);
        when(shopperApiService.getShopper(eq(godaddyShopper.getShopperId()),anyString())).thenReturn(godaddyShopper);
        when(shopperApiService.getShopper(eq(brandResellerShopper.getShopperId()),anyString())).thenReturn(brandResellerShopper);
    }

    @Test
    public void testGetGoDaddyShopperByCustomerId() throws UnknownHostException {
        Shopper response = service.getShopperByCustomerId(godaddyCustomerId.toString());
        verify(shopperApiService, times(1)).getShopperByCustomerId(eq(godaddyCustomerId.toString()), anyString());

        assertEquals(godaddyCustomerId, response.getCustomerId());
        assertEquals(godaddyShopper.getShopperId(), response.getShopperId());
        assertNull(response.getParentShopperId());
        assertEquals(godaddyShopper.getPrivateLabelId(), response.getPrivateLabelId());
    }

    @Test
    public void testGetGoDaddyShopper() throws UnknownHostException {
        Shopper response = service.getShopper(gdShopperId);
        verify(shopperApiService, times(1)).getShopper(eq(gdShopperId), anyString());

        assertEquals(godaddyCustomerId, response.getCustomerId());
        assertEquals(gdShopperId, response.getShopperId());
        assertNull(response.getParentShopperId());
        assertEquals(godaddyShopper.getPrivateLabelId(), response.getPrivateLabelId());
    }

    @Test
    public void testGetBrandShopperByCustomerId() throws UnknownHostException {
        Shopper response = service.getShopperByCustomerId(brandCustomerId.toString());
        verify(shopperApiService, times(1)).getShopperByCustomerId(eq(brandCustomerId.toString()), anyString());

        assertEquals(brandCustomerId, response.getCustomerId());
        assertEquals(brandResellerShopper.getShopperId(), response.getShopperId());
        assertEquals(brandResellerShopper.getParentShopperId(), response.getParentShopperId());
        assertEquals(brandResellerShopper.getPrivateLabelId(), response.getPrivateLabelId());
    }

    @Test
    public void testGetBrandShopper() throws UnknownHostException {
        Shopper response = service.getShopper(brandShopperId);
        verify(shopperApiService, times(1)).getShopper(eq(brandShopperId), anyString());

        assertEquals(brandCustomerId, response.getCustomerId());
        assertEquals(brandShopperId, response.getShopperId());
        assertEquals(brandResellerShopper.getParentShopperId(), response.getParentShopperId());
        assertEquals(brandResellerShopper.getPrivateLabelId(), response.getPrivateLabelId());
    }
}
