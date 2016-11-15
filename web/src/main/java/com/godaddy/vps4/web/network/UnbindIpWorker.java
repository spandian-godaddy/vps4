package com.godaddy.vps4.web.network;

import java.util.concurrent.Callable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.godaddy.vps4.vm.ActionStatus;
import com.godaddy.vps4.Vps4Exception;

import gdg.hfs.vhfs.network.AddressAction;
import gdg.hfs.vhfs.network.NetworkService;

public class UnbindIpWorker implements Callable<NetworkAction> {

    private static final Logger logger = LoggerFactory.getLogger(UnbindIpWorker.class);

    final NetworkService networkService;
    final NetworkAction action;

    public UnbindIpWorker(NetworkAction action, NetworkService networkService) {
        this.networkService = networkService;
        this.action = action;
    }

    @Override
    public NetworkAction call() {
        logger.info("sending HFS request to unbind addressId {}", action.addressId);

        AddressAction hfsAction = networkService.unbindIp(action.addressId);

        while (!hfsAction.status.equals(AddressAction.Status.COMPLETE) && !hfsAction.status.equals(AddressAction.Status.FAILED)) {
            logger.info("waiting on unbind ip: {}", hfsAction);

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
            throw new Vps4Exception("UNBIND_IP_FAILED", String.format("Unbind IP %d failed", action.addressId));
        }
        else {
            action.status = ActionStatus.COMPLETE;
        }

        logger.info("unbind ip complete: {}", action);
        return action;
    }
}
