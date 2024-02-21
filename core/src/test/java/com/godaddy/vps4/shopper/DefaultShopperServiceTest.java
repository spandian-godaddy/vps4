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

    Shopper godaddyShopper = new Shopper(godaddyCustomerId, "fakeShopperId", null, "1");
    Shopper brandResellerShopper = new Shopper(brandCustomerId, "fakeShopperId", "123456", "232323");


    @Before
    public void setupTest() {
        service = new DefaultShopperService(shopperApiService);

        when(shopperApiService.getCustomer(eq(godaddyCustomerId.toString()),anyString())).thenReturn(godaddyShopper);
        when(shopperApiService.getCustomer(eq(brandCustomerId.toString()),anyString())).thenReturn(brandResellerShopper);
    }

    @Test
    public void testGetGoDaddyShopper() throws UnknownHostException {
        Shopper response = service.getCustomer(godaddyCustomerId.toString());
        verify(shopperApiService, times(1)).getCustomer(eq(godaddyCustomerId.toString()), anyString());

        assertEquals(godaddyCustomerId, response.getCustomerId());
        assertEquals(godaddyShopper.getShopperId(), response.getShopperId());
        assertNull(response.getParentShopperId());
        assertEquals(godaddyShopper.getPrivateLabelId(), response.getPrivateLabelId());
    }

    @Test
    public void testGetBrandShopper() throws UnknownHostException {
        Shopper response = service.getCustomer(brandCustomerId.toString());
        verify(shopperApiService, times(1)).getCustomer(eq(brandCustomerId.toString()), anyString());

        assertEquals(brandCustomerId, response.getCustomerId());
        assertEquals(brandResellerShopper.getShopperId(), response.getShopperId());
        assertEquals(brandResellerShopper.getParentShopperId(), response.getParentShopperId());
        assertEquals(brandResellerShopper.getPrivateLabelId(), response.getPrivateLabelId());
    }
}
