package com.godaddy.vps4.orchestration.vm;


import javax.inject.Inject;

import gdg.hfs.orchestration.CommandRetryStrategy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.godaddy.vps4.network.IpAddress;
import com.godaddy.vps4.network.NetworkService;
import com.godaddy.vps4.orchestration.ActionCommand;
import com.godaddy.vps4.vm.ActionService;
import com.godaddy.vps4.vm.VirtualMachineService;

import gdg.hfs.orchestration.CommandContext;
import gdg.hfs.orchestration.CommandMetadata;
import com.godaddy.hfs.vm.VmService;

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
        IpAddress ip = networkService.getIpAddress(request.ipAddressId);
        Vps4DestroyIpAddress.Request req = new Vps4DestroyIpAddress.Request(ip, request.virtualMachine, request.forceIfVmInaccessible);

        context.execute(Vps4DestroyIpAddress.class, req);

        context.execute("Destroy-"+ip.ipAddressId, ctx -> {networkService.destroyIpAddress(ip.ipAddressId);
        return null;}, Void.class);

        logger.info("Completed removing IP from vm {}", request.virtualMachine.vmId);

        return null;
    }

    public static class Request extends VmActionRequest{
        public long ipAddressId;
        public boolean forceIfVmInaccessible;
    }

}
