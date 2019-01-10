package com.godaddy.vps4.orchestration.hfs.network;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import gdg.hfs.orchestration.Command;
import gdg.hfs.orchestration.CommandContext;
import gdg.hfs.vhfs.network.AddressAction;
import gdg.hfs.vhfs.network.NetworkServiceV2;

public class BindIp implements Command<BindIp.BindIpRequest, Void> {

    private static final Logger logger = LoggerFactory.getLogger(BindIp.class);

    final NetworkServiceV2 networkService;

    @Inject
    public BindIp(NetworkServiceV2 networkService) {
        this.networkService = networkService;
    }

    @Override
    public Void execute(CommandContext context, BindIpRequest action) {

        logger.info("sending HFS request to bind addressId {} to vmId {}", action.addressId, action.vmId);

        boolean shouldForce = false;
        AddressAction hfsAction = context.execute("BindIpHfs",
                ctx -> networkService.bindIp(action.addressId, action.vmId, shouldForce),
                AddressAction.class);

        context.execute(WaitForAddressAction.class, hfsAction);

        return null;
    }

    public static class BindIpRequest {
        public long addressId;
        public long vmId;
    }
}
