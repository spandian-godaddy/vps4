package com.godaddy.vps4.shopper;

import com.godaddy.vps4.shopper.model.Shopper;

import java.net.UnknownHostException;

public interface ShopperService {
     Shopper getShopperByCustomerId(String customerId) throws UnknownHostException;
     Shopper getShopper(String shopperId) throws UnknownHostException;
}
