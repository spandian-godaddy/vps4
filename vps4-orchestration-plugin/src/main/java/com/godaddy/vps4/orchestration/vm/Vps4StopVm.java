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
        name="Vps4StopVm",
        requestType=Vps4StopVm.Request.class,
        responseType=Vps4StopVm.Response.class
    )
public class Vps4StopVm extends ActionCommand<Vps4StopVm.Request, Vps4StopVm.Response> {
    
    final ActionService actionService;
    final VmService vmService;

    @Inject
    public Vps4StopVm(ActionService actionService, VmService vmService) {
        super(actionService);
        this.actionService = actionService;
        this.vmService = vmService;
    }

    @Override
    protected Response executeWithAction(CommandContext context, Request request) throws Exception {
        long vmId = request.hfsVmId;
        
        VmAction hfsAction = context.execute("Vps4StopVm", ctx -> {
            return vmService.stopVm(vmId);
        });
        
        hfsAction = context.execute(WaitForManageVmAction.class, hfsAction);
        
        Vps4StopVm.Response response = new Vps4StopVm.Response();
        response.vmId = vmId;
        response.hfsAction = hfsAction;
        return response;
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
        public VmAction hfsAction;
    }
    
}
