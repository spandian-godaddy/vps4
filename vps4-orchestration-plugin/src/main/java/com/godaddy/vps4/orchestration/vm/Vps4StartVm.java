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
        name="Vps4StartVm",
        requestType=VmActionRequest.class,
        responseType=Vps4StartVm.Response.class,
        retryStrategy = CommandRetryStrategy.NEVER
    )
public class Vps4StartVm extends ActionCommand<VmActionRequest, Vps4StartVm.Response> {

    final ActionService actionService;
    final VmService vmService;

    @Inject
    public Vps4StartVm(ActionService actionService, VmService vmService) {
        super(actionService);
        this.actionService = actionService;
        this.vmService = vmService;
    }

    @Override
    protected Response executeWithAction(CommandContext context, VmActionRequest request) {
        VmAction hfsAction = context.execute("Vps4StartVm", ctx -> {
            return vmService.startVm(request.virtualMachine.hfsVmId);
        }, VmAction.class);

        hfsAction = context.execute(WaitForManageVmAction.class, hfsAction);

        Vps4StartVm.Response response = new Vps4StartVm.Response();
        response.vmId = request.virtualMachine.hfsVmId;
        response.hfsAction = hfsAction;
        return response;
    }

    public static class Response {
        public long vmId;
        public VmAction hfsAction;
    }

}
