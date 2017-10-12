package com.godaddy.vps4.orchestration.vm;

import javax.inject.Inject;

import com.godaddy.vps4.orchestration.ActionCommand;
import com.godaddy.vps4.vm.ActionService;

import gdg.hfs.orchestration.CommandContext;
import gdg.hfs.orchestration.CommandMetadata;
import gdg.hfs.vhfs.vm.VmAction;
import gdg.hfs.vhfs.vm.VmService;

@CommandMetadata(
        name="Vps4StartVm",
        requestType=VmActionRequest.class,
        responseType=Vps4StartVm.Response.class
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
    protected Response executeWithAction(CommandContext context, VmActionRequest request) throws Exception {
        long vmId = request.hfsVmId;

        VmAction hfsAction = context.execute("Vps4StartVm", ctx -> {
            return vmService.startVm(vmId);
        }, VmAction.class);

        hfsAction = context.execute(WaitForManageVmAction.class, hfsAction);

        Vps4StartVm.Response response = new Vps4StartVm.Response();
        response.vmId = vmId;
        response.hfsAction = hfsAction;
        return response;
    }

    public static class Response {
        public long vmId;
        public VmAction hfsAction;
    }

}