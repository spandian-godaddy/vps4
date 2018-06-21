package com.godaddy.vps4.orchestration.vm;

import javax.inject.Inject;

import com.godaddy.vps4.orchestration.ActionCommand;
import com.godaddy.vps4.orchestration.hfs.vm.StartVm;
import com.godaddy.vps4.orchestration.hfs.vm.StopVm;
import com.godaddy.vps4.vm.ActionService;

import gdg.hfs.orchestration.CommandContext;
import gdg.hfs.orchestration.CommandMetadata;
import gdg.hfs.vhfs.vm.VmAction;
import gdg.hfs.vhfs.vm.VmService;

@CommandMetadata(
        name="Vps4RestartVm",
        requestType=VmActionRequest.class,
        responseType=Vps4RestartVm.Response.class
    )
public class Vps4RestartVm extends ActionCommand<VmActionRequest, Vps4RestartVm.Response> {

    final ActionService actionService;
    final VmService vmService;

    @Inject
    public Vps4RestartVm(ActionService actionService, VmService vmService) {
        super(actionService);
        this.actionService = actionService;
        this.vmService = vmService;
    }

    @Override
    protected Response executeWithAction(CommandContext context, VmActionRequest request) throws Exception {

        VmAction hfsAction = context.execute(StopVm.class, request.virtualMachine.hfsVmId);

        hfsAction = context.execute(StartVm.class, request.virtualMachine.hfsVmId);

        Vps4RestartVm.Response response = new Vps4RestartVm.Response();
        response.vmId = request.virtualMachine.hfsVmId;
        response.hfsAction = hfsAction;
        return response;
    }

    public static class Response {
        public long vmId;
        public VmAction hfsAction;
    }

}
