package com.godaddy.vps4.web.network;

import java.util.concurrent.Callable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.godaddy.vps4.Vps4Exception;
import com.godaddy.vps4.web.Action.ActionStatus;

import gdg.hfs.vhfs.network.AddressAction;
import gdg.hfs.vhfs.network.NetworkService;

public class ReleaseIpWorker implements Callable<NetworkAction> {

    private static final Logger logger = LoggerFactory.getLogger(ReleaseIpWorker.class);

    final NetworkService hfsNetworkService;
    final com.godaddy.vps4.network.NetworkService vps4NetworkService;
    final NetworkAction action;

    public ReleaseIpWorker(NetworkAction networkAction, NetworkService networkService,
            com.godaddy.vps4.network.NetworkService vps4NetworkService) {
        this.hfsNetworkService = networkService;
        this.action = networkAction;
        this.vps4NetworkService = vps4NetworkService;
    }

    @Override
    public NetworkAction call() {
        logger.info("sending HFS request to release addressId {}", action.addressId);

        AddressAction hfsAction = hfsNetworkService.releaseIp(action.addressId);

        while (!hfsAction.status.equals(AddressAction.Status.COMPLETE) && !hfsAction.status.equals(AddressAction.Status.FAILED)) {
            logger.info("waiting on release ip: {}", hfsAction);

            try {
                Thread.sleep(2000);
            }
            catch (InterruptedException e) {
                logger.warn("Interrupted while sleeping");
            }

            hfsAction = hfsNetworkService.getAddressAction(hfsAction.addressId, hfsAction.addressActionId);
        }

        if (!hfsAction.status.equals(AddressAction.Status.COMPLETE)) {
            action.status = ActionStatus.ERROR;
            throw new Vps4Exception("RELEASE_IP_FAILED", String.format("Release IP %d failed", action.addressId));
        }

        vps4NetworkService.destroyIpAddress(action.addressId);
        action.status = ActionStatus.COMPLETE;
        logger.info("release ip complete: {}", hfsAction);
        return action;
    }
}
