package com.godaddy.vps4.orchestration.vm;

import java.util.List;

import javax.inject.Inject;

import com.godaddy.vps4.network.IpAddress;
import com.godaddy.vps4.network.NetworkService;
import com.godaddy.vps4.orchestration.ActionCommand;
import com.godaddy.vps4.orchestration.ActionRequest;
import com.godaddy.vps4.orchestration.hfs.network.DestroyIpAddress;
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
        final long hfsVmId = request.hfsVmId;
        VirtualMachine vm = this.virtualMachineService.getVirtualMachine(hfsVmId); 

        List<IpAddress> addresses = networkService.getVmIpAddresses(vm.vmId);

        for (IpAddress address : addresses) {
            context.execute("DeleteIpAddress"+address.ipAddressId, DestroyIpAddress.class, new DestroyIpAddress.Request(address, vm));
            context.execute("Destroy-"+address.ipAddressId, ctx -> {networkService.destroyIpAddress(address.ipAddressId); 
                                                                    return null;});
        }
        
        context.execute("DestroyVmHfs", DestroyVm.class, hfsVmId);

        context.execute("Vps4DestroyVm", ctx -> {
            virtualMachineService.destroyVirtualMachine(hfsVmId);
            return null;
        });

        return null;
    }
    
    public static class Request implements ActionRequest{
        public long hfsVmId;
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
