package com.godaddy.vps4.orchestration.hfs.network;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import gdg.hfs.orchestration.Command;
import gdg.hfs.orchestration.CommandContext;
import gdg.hfs.vhfs.network.AddressAction;
import gdg.hfs.vhfs.network.NetworkServiceV2;

public class ReleaseIp implements Command<Long, Void> {

    private static final Logger logger = LoggerFactory.getLogger(ReleaseIp.class);

    final NetworkServiceV2 networkService;

    @Inject
    public ReleaseIp(NetworkServiceV2 networkService) {
        this.networkService = networkService;
    }

    @Override
    public Void execute(CommandContext context, Long addressId) {

        AddressAction action = context.execute("ReleaseIpHfs", ctx -> {
            logger.info("sending HFS request to release addressId {}", addressId);
            return networkService.releaseIp(addressId);
        }, AddressAction.class);

        context.execute(WaitForAddressAction.class, action);

        return null;
    }

}
