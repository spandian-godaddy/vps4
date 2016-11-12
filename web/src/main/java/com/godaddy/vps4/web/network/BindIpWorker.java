package com.godaddy.vps4.web.network;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.godaddy.vps4.Vps4Exception;
import com.godaddy.vps4.web.Action.ActionStatus;

import gdg.hfs.vhfs.network.AddressAction;
import gdg.hfs.vhfs.network.NetworkService;

public class BindIpWorker implements Runnable {

    private static final Logger logger = LoggerFactory.getLogger(BindIpWorker.class);

    final NetworkService networkService;
    final com.godaddy.vps4.network.NetworkService vps4NetworkService;
    final BindIpAction action;

    public BindIpWorker(BindIpAction action, NetworkService networkService, com.godaddy.vps4.network.NetworkService vps4NetworkService) {
        this.networkService = networkService;
        this.vps4NetworkService = vps4NetworkService;
        this.action = action;
    }

    @Override
    public void run() {
        logger.info("sending HFS request to bind addressId {} to vmId {}", action.getAddressId(), action.getVmId());

        AddressAction hfsAction = networkService.bindIp(action.getAddressId(), action.getVmId());

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

        if (!hfsAction.status.equals(AddressAction.Status.COMPLETE)) {
            action.status = ActionStatus.ERROR;
            throw new Vps4Exception("BIND_IP_FAILED",
                    String.format("Bind IP %d failed for VM %d", action.getAddressId(), action.getVmId()));
        }

        vps4NetworkService.createIpAddress(action.getAddressId(), action.getVmId(), action.getAddress(), action.getType());
        action.status = ActionStatus.COMPLETE;
        logger.info("bind ip complete: {}", hfsAction);
    }
}
