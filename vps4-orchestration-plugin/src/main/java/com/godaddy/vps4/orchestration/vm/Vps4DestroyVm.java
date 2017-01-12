package com.godaddy.vps4.orchestration.vm;

import java.util.List;

import javax.inject.Inject;

import com.godaddy.vps4.network.IpAddress;
import com.godaddy.vps4.network.NetworkService;
import com.godaddy.vps4.orchestration.ActionCommand;
import com.godaddy.vps4.orchestration.ActionRequest;
import com.godaddy.vps4.orchestration.hfs.network.ReleaseIp;
import com.godaddy.vps4.orchestration.hfs.network.UnbindIp;
import com.godaddy.vps4.orchestration.hfs.vm.DestroyVm;
import com.godaddy.vps4.vm.ActionService;
import com.godaddy.vps4.vm.VirtualMachine;
import com.godaddy.vps4.vm.VirtualMachineService;

import gdg.hfs.orchestration.CommandContext;
import gdg.hfs.orchestration.CommandMetadata;


@CommandMetadata(
        name="Vps4DestroyVm",
        requestType=Vps4DestroyVm.Request.class,
        responseType=Vps4DestroyVm.Response.class
    )
public class Vps4DestroyVm extends ActionCommand<Vps4DestroyVm.Request, Vps4DestroyVm.Response> {

    final NetworkService networkService;

    final VirtualMachineService virtualMachineService;

    @Inject
    public Vps4DestroyVm(ActionService actionService,
            NetworkService networkService, VirtualMachineService virtualMachineService) {
        super(actionService);
        this.networkService = networkService;
        this.virtualMachineService = virtualMachineService;
    }

    @Override
    public Response executeWithAction(CommandContext context, Vps4DestroyVm.Request request) {

        final long vmId = request.vmId;
        VirtualMachine vm = this.virtualMachineService.getVirtualMachine(vmId); 

        List<IpAddress> addresses = networkService.getVmIpAddresses(vm.vmId);

        for (IpAddress address : addresses) {
            context.execute("Unbind-"+address.ipAddressId, UnbindIp.class, address.ipAddressId);
            context.execute("Release-"+address.ipAddressId, ReleaseIp.class, address.ipAddressId);
            context.execute("Destroy-"+address.ipAddressId, ctx -> {networkService.destroyIpAddress(address.ipAddressId); 
                                                                    return null;});
        }

        context.execute("DestroyVmHfs", DestroyVm.class, vmId);

        context.execute("Vps4DestroyVm", ctx -> {
            virtualMachineService.destroyVirtualMachine(vmId);
            return null;
        });

        return null;
    }
    
    public static class Request implements ActionRequest{
        public long vmId;
        public long actionId;

        @Override
        public long getActionId() {
            return actionId;
        }
    }
    
    public static class Response {
        public long vmId;
    }

}
