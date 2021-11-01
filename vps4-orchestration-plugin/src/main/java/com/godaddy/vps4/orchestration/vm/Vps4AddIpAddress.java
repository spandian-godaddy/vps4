package com.godaddy.vps4.orchestration.vm;


import java.util.UUID;

import javax.inject.Inject;

import com.godaddy.vps4.orchestration.ActionRequest;
import gdg.hfs.orchestration.CommandRetryStrategy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.godaddy.vps4.network.IpAddress.IpAddressType;
import com.godaddy.vps4.network.NetworkService;
import com.godaddy.vps4.orchestration.ActionCommand;
import com.godaddy.vps4.orchestration.hfs.mailrelay.SetMailRelayQuota;
import com.godaddy.vps4.orchestration.hfs.network.AllocateIp;
import com.godaddy.vps4.vm.ActionService;
import com.godaddy.vps4.vm.VirtualMachine;
import com.godaddy.vps4.vm.VirtualMachineService;

import gdg.hfs.orchestration.CommandContext;
import gdg.hfs.orchestration.CommandMetadata;
import gdg.hfs.vhfs.network.IpAddress;
import com.godaddy.hfs.vm.VmService;

@CommandMetadata(
        name = "Vps4AddIpAddress",
        requestType = Vps4AddIpAddress.Request.class,
        retryStrategy = CommandRetryStrategy.NEVER
)
public class Vps4AddIpAddress extends ActionCommand<Vps4AddIpAddress.Request, Void> {

    private static final Logger logger = LoggerFactory.getLogger(Vps4AddIpAddress.class);

    final ActionService actionService;
    final VmService vmService;
    final VirtualMachineService virtualMachineService;
    final NetworkService networkService;

    @Inject
    public Vps4AddIpAddress(ActionService actionService, VmService vmService,
                            VirtualMachineService virtualMachineService, NetworkService networkService) {
        super(actionService);
        this.actionService = actionService;
        this.vmService = vmService;
        this.virtualMachineService = virtualMachineService;
        this.networkService = networkService;
    }

    @Override
    protected Void executeWithAction(CommandContext context, Vps4AddIpAddress.Request request) throws Exception {
        logger.info("Add an IP to vm {}", request.vmId);
        VirtualMachine virtualMachine = virtualMachineService.getVirtualMachine(request.vmId);
        IpAddress hfsIp = allocateIp(context, request);
        addIpToDatabase(context, hfsIp, virtualMachine.vmId);

        if (virtualMachine.spec.isVirtualMachine() && request.internetProtocolVersion == 4) {
            disableMailRelays(context, hfsIp);
        }

        logger.info("Completed adding HFS IP {} to vm {}", hfsIp.address, virtualMachine.vmId);

        return null;
    }

    private IpAddress allocateIp(CommandContext context,
                                 Vps4AddIpAddress.Request request) {
        AllocateIp.Request allocateIpRequest = new AllocateIp.Request();
        allocateIpRequest.sgid = request.sgid;
        allocateIpRequest.zone = request.zone;
        allocateIpRequest.serverId = request.serverId;
        allocateIpRequest.internetProtocolVersion = request.internetProtocolVersion;

        logger.info("Allocating IP for sgid {} in zone {}", allocateIpRequest.sgid, allocateIpRequest.zone);
        return context.execute(AllocateIp.class, allocateIpRequest);
    }

    private void addIpToDatabase(CommandContext context, IpAddress hfsIp, UUID vmId) {
        logger.info("Adding HFS IP {} to the db for vmId {}", hfsIp.address, vmId);
        context.execute("Create-" + hfsIp.addressId, ctx -> {
            networkService.createIpAddress(hfsIp.addressId, vmId, hfsIp.address, IpAddressType.SECONDARY);
            return null;
        }, Void.class);
    }

    private void disableMailRelays(CommandContext context, IpAddress hfsIp) {
        SetMailRelayQuota.Request hfsRequest = new SetMailRelayQuota.Request();
        hfsRequest.ipAddress = hfsIp.address;
        hfsRequest.mailRelayQuota = 0;
        context.execute(SetMailRelayQuota.class, hfsRequest);
    }

    public static class Request implements ActionRequest {
        public UUID vmId;
        public long actionId;
        public String sgid;
        public String zone;
        public long serverId;
        public int internetProtocolVersion;

        @Override
        public long getActionId() {
            return actionId;
        }

        @Override
        public void setActionId(long actionId) {
            this.actionId = actionId;
        }
    }
}
