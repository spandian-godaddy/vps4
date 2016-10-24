package com.godaddy.vps4.web.network;

import com.godaddy.vps4.web.Action;

public class NetworkAction extends Action {
    public NetworkAction(long ipAddressId) {
        this.addressId = ipAddressId;
    }

    public long addressId;

    @Override
    public String toString() {
        return String.format("NetworkAction [actionId=%d, addressId=%d, status=%s]", actionId, addressId, status);
    }
}
