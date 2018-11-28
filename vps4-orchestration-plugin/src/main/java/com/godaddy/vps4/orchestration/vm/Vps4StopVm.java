package com.godaddy.vps4.orchestration.vm;

import javax.inject.Inject;

import com.godaddy.vps4.orchestration.ActionCommand;
import com.godaddy.vps4.vm.ActionService;

import gdg.hfs.orchestration.CommandContext;
import gdg.hfs.orchestration.CommandMetadata;
import gdg.hfs.orchestration.CommandRetryStrategy;
import com.godaddy.hfs.vm.VmAction;
import com.godaddy.hfs.vm.VmService;


@CommandMetadata(
        name="Vps4StopVm",
        requestType=VmActionRequest.class,
        responseType=Vps4StopVm.Response.class,
        retryStrategy = CommandRetryStrategy.NEVER
    )
public class Vps4StopVm extends ActionCommand<VmActionRequest, Vps4StopVm.Response> {

    final ActionService actionService;
    final VmService vmService;

    @Inject
    public Vps4StopVm(ActionService actionService, VmService vmService) {
        super(actionService);
        this.actionService = actionService;
        this.vmService = vmService;
    }

    @Override
    protected Response executeWithAction(CommandContext context, VmActionRequest request) {

        VmAction hfsAction = context.execute("Vps4StopVm", ctx -> {
            return vmService.stopVm(request.virtualMachine.hfsVmId);
        }, VmAction.class);

        hfsAction = context.execute(WaitForManageVmAction.class, hfsAction);

        Vps4StopVm.Response response = new Vps4StopVm.Response();
        response.vmId = request.virtualMachine.hfsVmId;
        response.hfsAction = hfsAction;
        return response;
    }

    public static class Response {
        public long vmId;
        public VmAction hfsAction;
    }

}
