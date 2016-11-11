package com.godaddy.vps4.web.network;

import com.godaddy.vps4.network.IpAddress.IpAddressType;
import com.godaddy.vps4.web.Action;

public class BindIpAction extends Action {

    private final long addressId;
    private final String address;
    private final long vmId;
    private final IpAddressType type;

    public BindIpAction(long addressId, String address, long vmId, IpAddressType type) {
        this.addressId = addressId;
        this.address = address;
        this.vmId = vmId;
        this.type = type;
    }

    public long getAddressId() {
        return addressId;
    }

    public String getAddress() {
        return address;
    }

    public long getVmId() {
        return vmId;
    }

    public IpAddressType getType() {
        return type;
    }

}
