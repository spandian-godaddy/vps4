package com.godaddy.vps4.orchestration.hfs.network;

import static gdg.hfs.vhfs.network.IpAddress.Status.RELEASED;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import gdg.hfs.orchestration.Command;
import gdg.hfs.orchestration.CommandContext;
import gdg.hfs.vhfs.network.AddressAction;
import gdg.hfs.vhfs.network.IpAddress;
import gdg.hfs.vhfs.network.NetworkServiceV2;

public class ReleaseIp implements Command<Long, Void> {

    private static final Logger logger = LoggerFactory.getLogger(ReleaseIp.class);

    final NetworkServiceV2 networkService;

    private long hfsAddressId;

    @Inject
    public ReleaseIp(NetworkServiceV2 networkService) {
        this.networkService = networkService;
    }

    @Override
    public Void execute(CommandContext context, Long addressId) {
        this.hfsAddressId = addressId;
        if (isAddressAlreadyReleased()) {
            return null;
        }

        releaseAddress(context);
        return null;
    }

    private boolean isAddressAlreadyReleased() {
        IpAddress hfsAddress = networkService.getAddress(hfsAddressId);
        return hfsAddress.status == RELEASED;
    }

    private void releaseAddress(CommandContext context) {
        logger.info("Releasing HFS address {}", hfsAddressId);
        AddressAction action = context.execute("ReleaseIpHfs",
                ctx -> networkService.releaseIp(hfsAddressId),
                AddressAction.class);

        context.execute(WaitForAddressAction.class, action);
    }

}
