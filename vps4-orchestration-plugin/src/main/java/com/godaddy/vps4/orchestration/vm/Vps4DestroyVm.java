package com.godaddy.vps4.orchestration.vm;

import java.util.List;
import java.util.UUID;

import javax.inject.Inject;

import com.godaddy.vps4.network.IpAddress;
import com.godaddy.vps4.network.NetworkService;
import com.godaddy.vps4.orchestration.ActionCommand;
import com.godaddy.vps4.orchestration.ActionRequest;
import com.godaddy.vps4.orchestration.hfs.cpanel.WaitForCpanelAction;
import com.godaddy.vps4.orchestration.hfs.pingcheck.DeleteCheck;
import com.godaddy.vps4.orchestration.hfs.vm.DestroyVm;
import com.godaddy.vps4.vm.ActionService;
import com.godaddy.vps4.vm.VirtualMachine;
import com.godaddy.vps4.vm.VirtualMachineService;

import gdg.hfs.orchestration.CommandContext;
import gdg.hfs.orchestration.CommandMetadata;
import gdg.hfs.vhfs.cpanel.CPanelAction;
import gdg.hfs.vhfs.cpanel.CPanelService;
import gdg.hfs.vhfs.vm.Vm;
import gdg.hfs.vhfs.vm.VmService;

@CommandMetadata(
        name="Vps4DestroyVm",
        requestType=Vps4DestroyVm.Request.class,
        responseType=Vps4DestroyVm.Response.class
    )
public class Vps4DestroyVm extends ActionCommand<Vps4DestroyVm.Request, Vps4DestroyVm.Response> {

    final NetworkService networkService;

    final VirtualMachineService virtualMachineService;
    
    final VmService vmService;
    
    final CPanelService cpanelService;

    @Inject
    public Vps4DestroyVm(ActionService actionService,
            NetworkService networkService, 
            VirtualMachineService virtualMachineService,
            VmService vmService,
            CPanelService cpanelService) {
        super(actionService);
        this.networkService = networkService;
        this.virtualMachineService = virtualMachineService;
        this.vmService = vmService;
        this.cpanelService = cpanelService;
    }
    
  
    @Override
    public Response executeWithAction(CommandContext context, Vps4DestroyVm.Request request) {
        final long hfsVmId = request.hfsVmId;
        VirtualMachine vm = this.virtualMachineService.getVirtualMachine(hfsVmId); 
        
        unlicenseCpanel(context, hfsVmId, vm.vmId);
        
        List<IpAddress> addresses = networkService.getVmIpAddresses(vm.vmId);

        for (IpAddress address : addresses) {
            if(address.pingCheckId != null ){
                DeleteCheck.Request deleteCheckRequest = new DeleteCheck.Request(request.pingCheckAccountId, address.pingCheckId);
                context.execute(DeleteCheck.class, deleteCheckRequest);
            }
            
            context.execute("DeleteIpAddress-"+address.ipAddressId, Vps4DestroyIpAddress.class, new Vps4DestroyIpAddress.Request(address, vm));
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

    private void unlicenseCpanel(CommandContext context, final long hfsVmId, UUID vmId) {
        if(this.virtualMachineService.virtualMachineHasCpanel(vmId)){
            Vm hfsVm = vmService.getVm(hfsVmId);
            CPanelAction action = context.execute("Unlicense-Cpanel", ctx -> {
                return cpanelService.licenseRelease(hfsVmId, hfsVm.address.ip_address);
            });
            context.execute(WaitForCpanelAction.class, action);
        }
    }
    
    public static class Request implements ActionRequest{
        public long hfsVmId;
        public long actionId;
        public long pingCheckAccountId;

        @Override
        public long getActionId() {
            return actionId;
        }
    }
    
    public static class Response {
        public long vmId;
    }

}
