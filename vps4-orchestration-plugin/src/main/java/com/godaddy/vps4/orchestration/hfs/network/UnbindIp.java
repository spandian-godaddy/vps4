package com.godaddy.vps4.orchestration.hfs.network;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import gdg.hfs.orchestration.Command;
import gdg.hfs.orchestration.CommandContext;
import gdg.hfs.vhfs.network.AddressAction;
import gdg.hfs.vhfs.network.IpAddress;
import gdg.hfs.vhfs.network.NetworkService;
import gdg.hfs.vhfs.network.IpAddress.Status;


public class UnbindIp implements Command<UnbindIp.Request, Void> {

    private static final Logger logger = LoggerFactory.getLogger(UnbindIp.class);

    final NetworkService networkService;

    @Inject
    public UnbindIp(NetworkService networkService) {
        this.networkService = networkService;
    }

    @Override
    public Void execute(CommandContext context, Request request) {
        IpAddress ipAddress = networkService.getAddress(request.addressId);
        if(ipAddress.status != Status.BOUND) {
            logger.info("IP Address {} is {}, no need to unbind", request.addressId, ipAddress.status);
            return null;
        }

        logger.info("sending HFS request to unbind addressId {}", request.addressId);

        AddressAction hfsAction = networkService.unbindIp(request.addressId, request.forceIfVmInaccessible);
        context.execute(WaitForAddressAction.class, hfsAction);

        return null;
    }

    public static class Request {
        public Long addressId;
        public boolean forceIfVmInaccessible;
    }

}
