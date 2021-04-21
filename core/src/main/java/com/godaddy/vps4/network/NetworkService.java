package com.godaddy.vps4.network;

import java.util.List;
import java.util.UUID;

import com.godaddy.vps4.network.IpAddress.IpAddressType;

public interface NetworkService {

    void createIpAddress(long ipAddressId, UUID vmId, String ipAddress, IpAddressType ipAddressType);

    void destroyIpAddress(long ipAddressId);

    IpAddress getIpAddress(long ipAddressId);

    List<IpAddress> getVmIpAddresses(UUID vmId);

    IpAddress getVmPrimaryAddress(UUID vmId);
    
    IpAddress getVmPrimaryAddress(long hfsVmId);

    List<IpAddress> getVmSecondaryAddress(long hfsVmId);

    int getActiveIpAddressesCount(UUID vmId);

    void updateIpWithCheckId(long addressId, long checkId);

    void updateIpWithCheckId(String ipAddress, long checkId);
}
