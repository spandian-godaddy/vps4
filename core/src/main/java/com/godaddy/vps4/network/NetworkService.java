package com.godaddy.vps4.network;

import java.util.List;

public interface NetworkService {

    void createIpAddress(long ipAddressId, long projectId);

    void destroyIpAddress(long ipAddressId);

    IpAddress getIpAddress(long ipAddressId);

    List<IpAddress> listIpAddresses(long sgid);
}
