package com.godaddy.vps4.orchestration.hfs.network;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.godaddy.vps4.network.IpAddressValidator;

import gdg.hfs.orchestration.Command;
import gdg.hfs.orchestration.CommandContext;
import gdg.hfs.vhfs.network.AddressAction;
import gdg.hfs.vhfs.network.IpAddress;
import gdg.hfs.vhfs.network.IpAddress.Status;
import gdg.hfs.vhfs.network.NetworkService;

public class AllocateIp implements Command<AllocateIp.Request, IpAddress> {

    private static final Logger logger = LoggerFactory.getLogger(AllocateIp.class);

    public static class Request {
        public String sgid;
        public String zone;
    }

    final NetworkService networkService;

    @Inject
    public AllocateIp(NetworkService networkService) {
        this.networkService = networkService;
    }

    @Override
    public IpAddress execute(CommandContext context, AllocateIp.Request request) {

        logger.info("sending HFS request to allocate IP for hfsSgid: {}", request.sgid);

        AddressAction hfsAction = context.execute("RequestFromHFS",
                ctx -> networkService.acquireIp(request.sgid, request.zone),
                AddressAction.class);

        context.execute(WaitForAddressAction.class, hfsAction);

        IpAddress ipAddress = networkService.getAddress(hfsAction.addressId);
        IpAddressValidator.validateIpAddress(ipAddress.address);

        if (ipAddress.status != Status.UNBOUND) {
            throw new RuntimeException(String.format("IP %s is not unbound", ipAddress.address));
        }

        logger.info("Address allocate is complete: {}", hfsAction);
        logger.info("Allocated address: {}", ipAddress);

        return ipAddress;
    }
}
