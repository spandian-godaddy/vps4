package com.godaddy.vps4.orchestration.vm;


import javax.inject.Inject;

import com.godaddy.vps4.orchestration.ActionRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.godaddy.hfs.vm.VmService;
import com.godaddy.vps4.network.IpAddress;
import com.godaddy.vps4.network.NetworkService;
import com.godaddy.vps4.orchestration.ActionCommand;
import com.godaddy.vps4.vm.ActionService;
import com.godaddy.vps4.vm.VirtualMachineService;

import gdg.hfs.orchestration.CommandContext;
import gdg.hfs.orchestration.CommandMetadata;
import gdg.hfs.orchestration.CommandRetryStrategy;

import java.util.UUID;

@CommandMetadata(
        name="Vps4DestroyIpAddressAction",
        requestType=Vps4DestroyIpAddressAction.Request.class,
        retryStrategy = CommandRetryStrategy.NEVER
    )
public class Vps4DestroyIpAddressAction extends ActionCommand<Vps4DestroyIpAddressAction.Request, Void> {

    private static final Logger logger = LoggerFactory.getLogger(Vps4DestroyIpAddressAction.class);

    final ActionService actionService;
    final VmService vmService;
    final VirtualMachineService virtualMachineService;
    final NetworkService networkService;

    @Inject
    public Vps4DestroyIpAddressAction(ActionService actionService, VmService vmService,
            VirtualMachineService virtualMachineService, NetworkService networkService) {
        super(actionService);
        this.actionService = actionService;
        this.vmService = vmService;
        this.virtualMachineService = virtualMachineService;
        this.networkService = networkService;
    }

    @Override
    protected Void executeWithAction(CommandContext context, Vps4DestroyIpAddressAction.Request request) throws Exception {
        IpAddress ip = networkService.getIpAddress(request.addressId);
        context.execute(Vps4RemoveIp.class, ip);

        context.execute("MarkIpDeleted-" + ip.addressId, ctx -> {
                networkService.destroyIpAddress(ip.addressId);
                return null;
            }, Void.class);

        logger.info("Completed removing IP {} from vm {}", request.addressId, request.vmId);

        return null;
    }

    public static class Request implements ActionRequest {
        public UUID vmId;
        public long addressId;
        public long actionId;
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
