package com.godaddy.vps4.network;

import java.util.List;
import java.util.UUID;

import com.godaddy.vps4.network.IpAddress.IpAddressType;

public interface NetworkService {

    IpAddress createIpAddress(long hfsAddressId, UUID vmId, String ipAddress, IpAddressType ipAddressType);

    void destroyIpAddress(long addressId);

    IpAddress getIpAddress(long addressId);

    List<IpAddress> getVmIpAddresses(UUID vmId);

    IpAddress getVmPrimaryAddress(UUID vmId);
    
    IpAddress getVmPrimaryAddress(long hfsVmId);

    List<IpAddress> getVmSecondaryAddress(long hfsVmId);

    int getActiveIpAddressesCount(UUID vmId);
}
