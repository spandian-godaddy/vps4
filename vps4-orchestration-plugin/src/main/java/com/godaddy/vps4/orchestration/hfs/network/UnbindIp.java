package com.godaddy.vps4.orchestration.hfs.network;

import static gdg.hfs.vhfs.network.IpAddress.Status.BINDING;
import static gdg.hfs.vhfs.network.IpAddress.Status.BOUND;
import static gdg.hfs.vhfs.network.IpAddress.Status.UNBINDING;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import gdg.hfs.orchestration.Command;
import gdg.hfs.orchestration.CommandContext;
import gdg.hfs.vhfs.network.AddressAction;
import gdg.hfs.vhfs.network.IpAddress;
import gdg.hfs.vhfs.network.NetworkServiceV2;


public class UnbindIp implements Command<UnbindIp.Request, Void> {

    private static final Logger logger = LoggerFactory.getLogger(UnbindIp.class);

    final NetworkServiceV2 networkService;

    private Request request;
    private IpAddress hfsAddress;

    @Inject
    public UnbindIp(NetworkServiceV2 networkService) {
        this.networkService = networkService;
    }

    @Override
    public Void execute(CommandContext context, Request request) {
        this.request = request;
        this.hfsAddress = networkService.getAddress(request.hfsAddressId);
        logger.info("Unbinding HFS Address {} with status {} from HFS server {}",
                hfsAddress.addressId, hfsAddress.status, hfsAddress.serverId);

        if (isHfsAddressUnbindNeeded()) {
            unbindAddress(context);
        } else {
            logger.info("HFS Unbind is unnecessary for HFS address {} with status {} - skipping",
                    hfsAddress.addressId, hfsAddress.status);
        }

        return null;
    }

    private boolean isHfsAddressUnbindNeeded() {
        return isHfsAddressBound() || isHfsAddressInBindTransition();
    }

    private boolean isHfsAddressBound() {
        return hfsAddress.status == BOUND;
    }

    private boolean isHfsAddressInBindTransition() {
        return hfsAddress.status == UNBINDING || hfsAddress.status == BINDING;
    }

    private void unbindAddress(CommandContext context) {
        if (isHfsAddressInBindTransition()) {
            validateCanForceUnbind();
        }

        AddressAction hfsAction = context.execute("RequestFromHFS", ctx -> {
            return networkService.unbindIp(request.hfsAddressId, request.forceIfVmInaccessible);
        }, AddressAction.class);

        context.execute(WaitForAddressAction.class, hfsAction);
    }

    private void validateCanForceUnbind() {
        if (!request.forceIfVmInaccessible) {
            throw new UnsupportedOperationException(String.format("Unforced Unbind - "
                    + "Cannot unbind HFS address (%d) with status (%s)", hfsAddress.addressId, hfsAddress.status));
        }
    }


    public static class Request {
        public long hfsAddressId;
        public boolean forceIfVmInaccessible;
    }

}
