package com.godaddy.vps4.orchestration.vm;

import com.godaddy.hfs.vm.VmAction;
import com.godaddy.hfs.vm.VmService;
import com.godaddy.vps4.orchestration.ActionCommand;
import com.godaddy.vps4.vm.ActionService;
import gdg.hfs.orchestration.CommandContext;
import gdg.hfs.orchestration.CommandMetadata;
import gdg.hfs.orchestration.CommandRetryStrategy;

import javax.inject.Inject;

@CommandMetadata(
        name="Vps4SyncVmStatus",
        requestType=VmActionRequest.class,
        responseType= Vps4SyncVmStatus.Response.class,
        retryStrategy = CommandRetryStrategy.NEVER
    )
public class Vps4SyncVmStatus extends ActionCommand<VmActionRequest, Vps4SyncVmStatus.Response> {

    final ActionService actionService;
    final VmService vmService;

    @Inject
    public Vps4SyncVmStatus(ActionService actionService, VmService vmService) {
        super(actionService);
        this.actionService = actionService;
        this.vmService = vmService;
    }

    @Override
    protected Response executeWithAction(CommandContext context, VmActionRequest request) {

        VmAction hfsAction = context.execute("Vps4SyncVmStatus", ctx -> {
            return vmService.sync(request.virtualMachine.hfsVmId);
        }, VmAction.class);

        hfsAction = context.execute(WaitForManageVmAction.class, hfsAction);

        Vps4SyncVmStatus.Response response = new Vps4SyncVmStatus.Response();
        response.vmId = request.virtualMachine.hfsVmId;
        response.hfsAction = hfsAction;
        return response;
    }

    public static class Response {
        public long vmId;
        public VmAction hfsAction;
    }

}
