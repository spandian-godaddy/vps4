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
import gdg.hfs.vhfs.network.NetworkServiceV2;

public class AllocateIp implements Command<AllocateIp.Request, IpAddress> {

    private static final Logger logger = LoggerFactory.getLogger(AllocateIp.class);

    public static class Request {
        public String sgid;
        public String zone;
        public Long serverId;
    }

    final NetworkServiceV2 networkService;

    @Inject
    public AllocateIp(NetworkServiceV2 networkService) {
        this.networkService = networkService;
    }

    @Override
    public IpAddress execute(CommandContext context, AllocateIp.Request request) {

        logger.info("sending HFS request to allocate IP for hfsSgid: {}", request.sgid);

        // HFS acquireIp API works differently based on platforms:
        // server id can be null for openstack vms
        // Openstack - allocates but doesn't bind or configure
        // OVH - allocates and binds but doesn't configure
        // Optimized Hosting - allocates and binds and configures

        AddressAction hfsAction = context.execute("RequestFromHFS",
                ctx -> networkService.acquireIp(request.sgid, request.zone, request.serverId),
                AddressAction.class);
        context.execute(WaitForAddressAction.class, hfsAction);

        IpAddress ipAddress = networkService.getAddress(hfsAction.addressId);
        IpAddressValidator.validateIpAddress(ipAddress.address);

        // hfs allocate ip for OH and OVH automatically updates status to BOUND
        // this check is for Openstack Ips that still needs to be manually bound
        // openstack ip allocation request always has null server id
        if (request.serverId == null && ipAddress.status != Status.UNBOUND) {
            throw new RuntimeException(String.format("IP %s is not unbound", ipAddress.address));
        }

        logger.info("Address allocate is complete: {}", hfsAction);
        logger.info("Allocated address: {}", ipAddress);

        return ipAddress;
    }
}
