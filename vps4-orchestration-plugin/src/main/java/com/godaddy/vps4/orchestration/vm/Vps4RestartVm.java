package com.godaddy.vps4.orchestration.vm;

import javax.inject.Inject;

import com.godaddy.vps4.orchestration.ActionCommand;
import com.godaddy.vps4.orchestration.ActionRequest;
import com.godaddy.vps4.vm.ActionService;

import gdg.hfs.orchestration.CommandContext;
import gdg.hfs.orchestration.CommandMetadata;
import gdg.hfs.vhfs.vm.VmAction;
import gdg.hfs.vhfs.vm.VmService;

@CommandMetadata(
        name="Vps4RestartVm",
        requestType=Vps4RestartVm.Request.class,
        responseType=Vps4RestartVm.Response.class
    )
public class Vps4RestartVm extends ActionCommand<Vps4RestartVm.Request, Vps4RestartVm.Response> {
    
    final ActionService actionService;
    final VmService vmService;

    @Inject
    public Vps4RestartVm(ActionService actionService, VmService vmService) {
        super(actionService);
        this.actionService = actionService;
        this.vmService = vmService;
    }

    @Override
    protected Response executeWithAction(CommandContext context, Request request) throws Exception {
        long vmId = request.vmId;
        
        VmAction hfsAction = context.execute("Vps4StopVm", ctx -> {
            return vmService.stopVm(vmId);
        });
        
        hfsAction = context.execute(WaitForManageVmAction.class, hfsAction);
        
        hfsAction = context.execute("Vps4StartVm", ctx -> {
            return vmService.startVm(vmId);
        });
        
        hfsAction = context.execute(WaitForManageVmAction.class, hfsAction);
        
        Vps4RestartVm.Response response = new Vps4RestartVm.Response();
        response.vmId = vmId;
        response.hfsAction = hfsAction;
        return response;
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
        public VmAction hfsAction;
    }
    
}
