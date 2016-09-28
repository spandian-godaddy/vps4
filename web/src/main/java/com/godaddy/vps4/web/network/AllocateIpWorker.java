package com.godaddy.vps4.web.network;

import java.util.concurrent.Callable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.godaddy.vps4.Vps4Exception;

import gdg.hfs.vhfs.network.AddressAction;
import gdg.hfs.vhfs.network.IpAddress;
import gdg.hfs.vhfs.network.IpAddress.Status;
import gdg.hfs.vhfs.network.NetworkService;

public class AllocateIpWorker implements Callable<IpAddress> {

    private static final Logger logger = LoggerFactory.getLogger(AllocateIpWorker.class);

    final NetworkService networkService;
    final String sgid;

    public AllocateIpWorker(NetworkService networkService, String sgid) {
        this.networkService = networkService;
        this.sgid = sgid;
    }

    @Override
    public IpAddress call() {

        logger.info("sending HFS request to allocate IP for hfsSgid: {}", sgid);

        AddressAction hfsAction = networkService.acquireIp(sgid);

        while (!hfsAction.status.equals(AddressAction.Status.COMPLETE) && !hfsAction.status.equals(AddressAction.Status.FAILED)) {
            logger.info("waiting on ip allocation: {}", hfsAction);

            try {
                Thread.sleep(2000);
            } catch (InterruptedException e) {
                logger.warn("Interrupted while sleeping");
            }

            hfsAction = networkService.getAddressAction(hfsAction.addressId, hfsAction.addressActionId);
        }

        if (hfsAction.status.equals(AddressAction.Status.COMPLETE)) {
            IpAddress ipAddress = networkService.getAddress(hfsAction.addressId);

            if (ipAddress.status != Status.UNBOUND) {
                throw new Vps4Exception("ALLOCATE_IP_FAILED", String.format("IP %s is not unbound", ipAddress.address));
            }

            logger.info("Address allocate is complete: {}", hfsAction);
            logger.info("Allocated address: {}", ipAddress);

            return ipAddress;
        }

        throw new Vps4Exception("ALLOCATE_IP_FAILED", String.format("Allocate IP failed for project %s", sgid));

    }

}
