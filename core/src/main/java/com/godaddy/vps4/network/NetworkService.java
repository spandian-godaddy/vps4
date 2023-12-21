package com.godaddy.vps4.network;

import java.util.List;
import java.util.UUID;

import com.godaddy.vps4.network.IpAddress.IpAddressType;

public interface NetworkService {

    IpAddress createIpAddress(long hfsAddressId, UUID vmId, String ipAddress, IpAddressType ipAddressType);

    void destroyIpAddress(long addressId);

    void activateIpAddress(long addressId);

    IpAddress getIpAddress(long addressId);

    List<IpAddress> getVmIpAddresses(UUID vmId);

    IpAddress getVmPrimaryAddress(UUID vmId);

    IpAddress getVmPrimaryAddress(long hfsVmId);

    List<IpAddress> getVmActiveSecondaryAddresses(long hfsVmId);

    List<IpAddress> getAllVmSecondaryAddresses(long hfsVmId);

    List<IpAddress> getActiveIpAddresses(long hfsVmId, int internetProtocolVersion);

    IpAddress getActiveIpAddressOfVm(UUID vmId, String ipAddress);

    int getActiveIpAddressesCount(UUID vmId, int internetProtocolVersion);
}