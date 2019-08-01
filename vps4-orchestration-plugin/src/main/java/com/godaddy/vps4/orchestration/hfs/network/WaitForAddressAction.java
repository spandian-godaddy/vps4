package com.godaddy.vps4.orchestration.hfs.network;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.godaddy.vps4.orchestration.scheduler.Utils;

import gdg.hfs.orchestration.Command;
import gdg.hfs.orchestration.CommandContext;
import gdg.hfs.vhfs.network.AddressAction;
import gdg.hfs.vhfs.network.NetworkServiceV2;

public class WaitForAddressAction implements Command<AddressAction, AddressAction> {

    private static final Logger logger = LoggerFactory.getLogger(WaitForAddressAction.class);

    final NetworkServiceV2 networkService;

    @Inject
    public WaitForAddressAction(NetworkServiceV2 networkService) {
        this.networkService = networkService;
    }

    @Override
    public AddressAction execute(CommandContext context, AddressAction hfsAction) {
        long addressId =  hfsAction.addressId;
        long addressActionId = hfsAction.addressActionId;

        while (!hfsAction.status.equals(AddressAction.Status.COMPLETE)
                && !hfsAction.status.equals(AddressAction.Status.FAILED)) {
            logger.debug("waiting for address action: {}", hfsAction);

            hfsAction = Utils.runWithRetriesForServerErrorException(context, logger, () ->{
                return networkService.getAddressAction(addressId, addressActionId);
            });
        }

        if (hfsAction.status != AddressAction.Status.COMPLETE) {
            throw new RuntimeException(String.format("failed to complete address action: %s", hfsAction));
        }

        return hfsAction;
    }

}
