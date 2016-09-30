package com.godaddy.vps4.web.network;

import java.util.concurrent.Callable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.godaddy.vps4.Vps4Exception;

import gdg.hfs.vhfs.network.AddressAction;
import gdg.hfs.vhfs.network.NetworkService;

public class BindIpWorker implements Callable<AddressAction> {

    private static final Logger logger = LoggerFactory.getLogger(BindIpWorker.class);

    final NetworkService networkService;
    final long addressId;
    final long vmId;

    public BindIpWorker(NetworkService networkService, long addressId, long vmId) {
        this.networkService = networkService;
        this.addressId = addressId;
        this.vmId = vmId;
    }

    @Override
    public AddressAction call() throws Exception {
        logger.info("sending HFS request to bind addressId {} to vmId {}", addressId, vmId);

        AddressAction hfsAction = networkService.bindIp(addressId, vmId);

        while (!hfsAction.status.equals(AddressAction.Status.COMPLETE) && !hfsAction.status.equals(AddressAction.Status.FAILED)) {
            logger.info("waiting on bind ip: {}", hfsAction);

            try {
                Thread.sleep(2000);
            }
            catch (InterruptedException e) {
                logger.warn("Interrupted while sleeping");
            }

            hfsAction = networkService.getAddressAction(hfsAction.addressId, hfsAction.addressActionId);
        }

        if (hfsAction.status.equals(AddressAction.Status.COMPLETE)) {
            logger.info("bind ip complete: {}", hfsAction);
        }
        else {
            throw new Vps4Exception("BIND_IP_FAILED", String.format("Bind IP %d failed for VM %d", addressId, vmId));
        }

        return hfsAction;
    }
}
