package com.godaddy.vps4.network;

import java.util.List;

import com.godaddy.vps4.network.IpAddress.IpAddressType;

public interface NetworkService {

    void createIpAddress(long ipAddressId, long vmId, String ipAddress, IpAddressType ipAddressType);

    void destroyIpAddress(long ipAddressId);

    IpAddress getIpAddress(long ipAddressId);

    List<IpAddress> getVmIpAddresses(long vmId);

    IpAddress getVmPrimaryAddress(long vmId);
}
