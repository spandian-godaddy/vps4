package com.godaddy.vps4.orchestration.vm;

import com.godaddy.hfs.vm.VmAction;
import com.godaddy.vps4.orchestration.ActionCommand;
import com.godaddy.vps4.orchestration.hfs.vm.RescueVm;
import com.godaddy.vps4.vm.ActionService;
import com.google.inject.Inject;

import gdg.hfs.orchestration.CommandContext;
import gdg.hfs.orchestration.CommandMetadata;
import gdg.hfs.orchestration.CommandRetryStrategy;

@CommandMetadata(
        name = "Vps4Rescue", 
        requestType = VmActionRequest.class, 
        responseType = VmAction.class, 
        retryStrategy = CommandRetryStrategy.NEVER)
public class Vps4Rescue extends ActionCommand<VmActionRequest, Vps4Rescue.Response> {

    @Inject
    public Vps4Rescue(ActionService actionService) {
        super(actionService);
    }

    @Override
    protected Response executeWithAction(CommandContext context, VmActionRequest request) throws Exception {
        VmAction action = context.execute(RescueVm.class, request.virtualMachine.hfsVmId);
        return new Response(action.vmActionId);
    }

    public static class Response {
        public long hfsVmActionId;

        public Response() {
        }
        
        public Response(long hfsVmActionId) {
            this.hfsVmActionId = hfsVmActionId;
        }
    }
}
