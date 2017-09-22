package com.godaddy.vps4.orchestration.hfs.network;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import gdg.hfs.orchestration.Command;
import gdg.hfs.orchestration.CommandContext;
import gdg.hfs.vhfs.network.AddressAction;
import gdg.hfs.vhfs.network.NetworkService;


public class UnbindIp implements Command<UnbindIp.Request, Void> {

    private static final Logger logger = LoggerFactory.getLogger(UnbindIp.class);

    final NetworkService networkService;

    @Inject
    public UnbindIp(NetworkService networkService) {
        this.networkService = networkService;
    }

    @Override
    public Void execute(CommandContext context, Request request) {
        logger.info("sending HFS request to unbind addressId {}", request.addressId);

        AddressAction hfsAction = context.execute("RequestFromHFS",  ctx -> {
            return networkService.unbindIp(request.addressId, request.forceIfVmInaccessible);
        });

        context.execute(WaitForAddressAction.class, hfsAction);

//        if (!hfsAction.status.equals(AddressAction.Status.COMPLETE)) {
//            action.status = ActionStatus.ERROR;
//            throw new Vps4Exception("UNBIND_IP_FAILED", String.format("Unbind IP %d failed", action.addressId));
//        }
//        else {
//            action.status = ActionStatus.COMPLETE;
//        }
        return null;
    }

    public static class Request {
        public Long addressId;
        public boolean forceIfVmInaccessible;
    }

}
