package com.godaddy.vps4.client;

import com.google.inject.Provider;
import com.godaddy.vps4.client.ThreadLocalShopperId;

// this provider simply returns the ThreadLocal shopper id value. If a shopper hasn't been set on the
// ThreadLocal variable then this provider can return a null
public class ShopperIdProvider implements Provider<String> {
    @Override
    public String get() {
        return ThreadLocalShopperId.get();
    }
}
