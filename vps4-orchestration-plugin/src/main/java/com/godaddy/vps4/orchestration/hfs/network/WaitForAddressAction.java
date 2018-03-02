package com.godaddy.vps4.orchestration.hfs.network;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import gdg.hfs.orchestration.Command;
import gdg.hfs.orchestration.CommandContext;
import gdg.hfs.vhfs.network.AddressAction;
import gdg.hfs.vhfs.network.NetworkService;

public class WaitForAddressAction implements Command<AddressAction, AddressAction> {

    private static final Logger logger = LoggerFactory.getLogger(WaitForAddressAction.class);

    final NetworkService networkService;

    @Inject
    public WaitForAddressAction(NetworkService networkService) {
        this.networkService = networkService;
    }

    @Override
    public AddressAction execute(CommandContext context, AddressAction hfsAction) {
        while (!hfsAction.status.equals(AddressAction.Status.COMPLETE)
                && !hfsAction.status.equals(AddressAction.Status.FAILED)) {
            logger.debug("waiting for address action: {}", hfsAction);

            context.sleep(2000);

            hfsAction = networkService.getAddressAction(hfsAction.addressId, hfsAction.addressActionId);
        }

        if (!hfsAction.status.equals(AddressAction.Status.COMPLETE)) {
            // FIXME update action status
            //action.status = ActionStatus.ERROR;
            //throw new Vps4Exception("BIND_IP_FAILED",
            //        String.format("Bind IP %d failed for VM %d", action.getAddressId(), action.getVmId()));
        }

        return hfsAction;
    }

}
