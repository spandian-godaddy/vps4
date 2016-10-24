package com.godaddy.vps4.web.network;

import com.godaddy.vps4.web.Action;

public class BindIpAction extends Action {

    private final long addressId;
    private final long vmId;

    public BindIpAction(long addressId, long vmId) {
        this.addressId = addressId;
        this.vmId = vmId;
    }

    public long getAddressId() {
        return addressId;
    }

    public long getVmId() {
        return vmId;
    }

}
