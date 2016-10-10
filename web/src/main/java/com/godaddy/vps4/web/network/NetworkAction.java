package com.godaddy.vps4.web.network;

import com.godaddy.vps4.web.Action;

public class NetworkAction extends Action {
    public NetworkAction(long ipAddressId) {
        this.addressId = ipAddressId;
        this.status = ActionStatus.IN_PROGRESS;

    }

    public long addressId;
    public volatile long hfsAddressActionId;

    @Override
    public String toString() {
        return String.format("NetworkAction [actionId=%d, addressId=%d, hfsAddressActionId=%d, status=%s]", actionId, addressId,
                hfsAddressActionId, status);
    }
}
