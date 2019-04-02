package com.godaddy.vps4.orchestration.hfs.network;

import static gdg.hfs.vhfs.network.IpAddress.Status.ACQUIRING;
import static gdg.hfs.vhfs.network.IpAddress.Status.BINDING;
import static gdg.hfs.vhfs.network.IpAddress.Status.BOUND;
import static gdg.hfs.vhfs.network.IpAddress.Status.RELEASED;
import static gdg.hfs.vhfs.network.IpAddress.Status.RELEASING;
import static gdg.hfs.vhfs.network.IpAddress.Status.UNBINDING;

import java.util.Arrays;
import java.util.List;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import gdg.hfs.orchestration.Command;
import gdg.hfs.orchestration.CommandContext;
import gdg.hfs.vhfs.network.AddressAction;
import gdg.hfs.vhfs.network.IpAddress;
import gdg.hfs.vhfs.network.IpAddress.Status;
import gdg.hfs.vhfs.network.NetworkServiceV2;

public class BindIp implements Command<BindIp.Request, Void> {

    private static final Logger logger = LoggerFactory.getLogger(BindIp.class);

    private final NetworkServiceV2 networkService;
    private final List<Status> invalidAddressStates = Arrays.asList(ACQUIRING, BINDING, RELEASED, RELEASING, UNBINDING);

    private Request request;
    private IpAddress hfsAddress;

    @Inject
    public BindIp(NetworkServiceV2 networkService) {
        this.networkService = networkService;
    }

    @Override
    public Void execute(CommandContext context, Request request) {
        this.request = request;
        this.hfsAddress = networkService.getAddress(request.addressId);
        logger.info("Binding HFS Address {} with status {} to HFS server {}",
                hfsAddress.addressId, hfsAddress.status, request.hfsVmId);

        validateHfsCanBindAddress();
        if (isAddressAlreadyBound()) {
            logger.info("HFS Address is already BOUND to HFS server {}", hfsAddress.serverId);
            if (isAddressBoundToRequestedServer()) {
                // Nothing to do, return successfully
                return null;
            }
            // Address NOT bound to requested server
            forceUnbindAddress(context);
        }
        bindAddress(context);
        return null;
    }

    private void validateHfsCanBindAddress() {
        if (invalidAddressStates.contains(hfsAddress.status)) {
            throw new IllegalStateException(String.format("Cannot bind HFS Address (%d) with status (%s)",
                    hfsAddress.addressId, hfsAddress.status));
        }
    }

    private boolean isAddressAlreadyBound() {
        return hfsAddress.status == BOUND;
    }

    private boolean isAddressBoundToRequestedServer() {
        return hfsAddress.serverId == request.hfsVmId;
    }

    private void forceUnbindAddress(CommandContext context) {
        validateCanForceUnbind();

        logger.info("Forcing unbind of HFS address {} from HFS server {}", hfsAddress.addressId, hfsAddress.serverId);
        UnbindIp.Request unbindRequest = new UnbindIp.Request();
        unbindRequest.addressId = hfsAddress.addressId;
        unbindRequest.forceIfVmInaccessible = true;
        context.execute(String.format("ForceUnbindIP-%d", hfsAddress.addressId), UnbindIp.class, unbindRequest);
    }

    private void validateCanForceUnbind() {
        if (!request.shouldForce) {
            throw new UnsupportedOperationException(String.format("Unforced Bind - "
                    + "Cannot bind HFS Address (%d) to HFS server (%s) already bound to HFS server (%d)",
                    hfsAddress.addressId, request.hfsVmId, hfsAddress.serverId));
        }
    }

    private void bindAddress(CommandContext context) {
        AddressAction hfsAction = context.execute("BindIpHfs",
                ctx -> networkService.bindIp(request.addressId, request.hfsVmId, request.shouldForce),
                AddressAction.class);

        context.execute(WaitForAddressAction.class, hfsAction);
    }


    public static class Request {
        public long addressId;
        public long hfsVmId;
        public boolean shouldForce;
    }
}
