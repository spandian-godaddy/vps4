package com.godaddy.vps4.orchestration.vm;

import com.godaddy.vps4.orchestration.ActionCommand;
import com.godaddy.vps4.vm.ActionService;
import gdg.hfs.orchestration.CommandContext;
import gdg.hfs.orchestration.CommandMetadata;
import gdg.hfs.orchestration.CommandRetryStrategy;
import gdg.hfs.vhfs.vm.VmAction;
import gdg.hfs.vhfs.vm.VmService;

import javax.inject.Inject;

@CommandMetadata(
        name="Vps4RestartDedVm",
        requestType= VmActionRequest.class,
        responseType= Vps4RebootDedicated.Response.class,
        retryStrategy = CommandRetryStrategy.NEVER
)
public class Vps4RebootDedicated extends ActionCommand<VmActionRequest, Vps4RebootDedicated.Response> {

    final ActionService actionService;
    final VmService vmService;

    @Inject
    public Vps4RebootDedicated(ActionService actionService, VmService vmService) {
        super(actionService);
        this.actionService = actionService;
        this.vmService = vmService;
    }

    @Override
    protected Vps4RebootDedicated.Response executeWithAction(CommandContext context, VmActionRequest request) {

        VmAction hfsAction = context.execute("Vps4RestartDed",
                ctx -> vmService.rebootVm(request.virtualMachine.hfsVmId),
                VmAction.class);

        hfsAction = context.execute("WaitForReboot", WaitForManageVmAction.class, hfsAction);

        Vps4RebootDedicated.Response response = new Vps4RebootDedicated.Response();
        response.vmId = request.virtualMachine.hfsVmId;
        response.hfsAction = hfsAction;
        return response;
    }

    public static class Response {
        public long vmId;
        public VmAction hfsAction;
    }


}
