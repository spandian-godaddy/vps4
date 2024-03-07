package com.godaddy.vps4.shopper;

import com.godaddy.vps4.shopper.model.Shopper;

import javax.inject.Inject;
import java.net.InetAddress;
import java.net.UnknownHostException;

public class DefaultShopperService implements ShopperService {
    private final ShopperApiService shopperApiService;

    @Inject
    public DefaultShopperService(ShopperApiService shopperApiService)
    {
        this.shopperApiService = shopperApiService;
    }

    @Override
    public Shopper getShopperByCustomerId(String customerId) throws UnknownHostException {
        return shopperApiService.getShopperByCustomerId(customerId, InetAddress.getLocalHost().getHostAddress());
    }

    @Override
    public Shopper getShopper(String shopperId) throws UnknownHostException {
        return shopperApiService.getShopper(shopperId, InetAddress.getLocalHost().getHostAddress());
    }
}
