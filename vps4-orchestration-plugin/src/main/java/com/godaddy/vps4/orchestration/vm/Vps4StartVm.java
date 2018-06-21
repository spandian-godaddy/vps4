package com.godaddy.vps4.orchestration.vm;

import javax.inject.Inject;

import com.godaddy.vps4.orchestration.ActionCommand;
import com.godaddy.vps4.orchestration.hfs.vm.StartVm;
import com.godaddy.vps4.vm.ActionService;

import gdg.hfs.orchestration.CommandContext;
import gdg.hfs.orchestration.CommandMetadata;
import gdg.hfs.vhfs.vm.VmAction;
import gdg.hfs.vhfs.vm.VmService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@CommandMetadata(
        name="Vps4StartVm",
        requestType=VmActionRequest.class,
        responseType=Vps4StartVm.Response.class
    )
public class Vps4StartVm extends ActionCommand<VmActionRequest, Vps4StartVm.Response> {

    private final Logger logger = LoggerFactory.getLogger(Vps4StartVm.class);

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

        logger.info("Vps4StartVm Request: {}", request);

        VmAction hfsAction = context.execute(StartVm.class, request.virtualMachine.hfsVmId);

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
