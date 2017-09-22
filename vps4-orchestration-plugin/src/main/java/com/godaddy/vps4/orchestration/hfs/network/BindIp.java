package com.godaddy.vps4.orchestration.hfs.network;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import gdg.hfs.orchestration.Command;
import gdg.hfs.orchestration.CommandContext;
import gdg.hfs.vhfs.network.AddressAction;
import gdg.hfs.vhfs.network.NetworkService;

public class BindIp implements Command<BindIp.BindIpRequest, Void> {

    private static final Logger logger = LoggerFactory.getLogger(BindIp.class);

    final NetworkService networkService;

    @Inject
    public BindIp(NetworkService networkService) {
        this.networkService = networkService;
    }

    @Override
    public Void execute(CommandContext context, BindIpRequest action) {

        logger.info("sending HFS request to bind addressId {} to vmId {}", action.addressId, action.vmId);

        AddressAction hfsAction = context.execute("BindIpHfs", ctx -> networkService.bindIp(action.addressId, action.vmId, false));

        context.execute(WaitForAddressAction.class, hfsAction);

        //vps4NetworkService.createIpAddress(action.getAddressId(), action.getVmId(), action.getAddress(), action.getType());
        //action.status = ActionStatus.COMPLETE;
        //logger.info("bind ip complete: {}", hfsAction);

        return null;
    }

    public static class BindIpRequest {
        public long addressId;
        public long vmId;
    }
}
