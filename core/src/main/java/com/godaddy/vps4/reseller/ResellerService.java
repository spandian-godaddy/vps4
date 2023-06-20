package com.godaddy.vps4.reseller;

import java.util.List;

public interface ResellerService {
    String getResellerDescription(String resellerId);
    List<String> getBrandResellerIds();
}
