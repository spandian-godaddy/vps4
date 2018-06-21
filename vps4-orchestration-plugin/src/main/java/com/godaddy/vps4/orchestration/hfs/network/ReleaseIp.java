package com.godaddy.vps4.orchestration.hfs.network;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import gdg.hfs.orchestration.Command;
import gdg.hfs.orchestration.CommandContext;
import gdg.hfs.vhfs.network.AddressAction;
import gdg.hfs.vhfs.network.NetworkService;

public class ReleaseIp implements Command<Long, Void> {

    private static final Logger logger = LoggerFactory.getLogger(ReleaseIp.class);

    final NetworkService networkService;

    @Inject
    public ReleaseIp(NetworkService networkService) {
        this.networkService = networkService;
    }

    @Override
    public Void execute(CommandContext context, Long addressId) {
        logger.info("sending HFS request to release addressId {}", addressId);
        AddressAction action = networkService.releaseIp(addressId);
        context.execute(WaitForAddressAction.class, action);
        return null;
    }

}
