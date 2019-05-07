package com.godaddy.vps4.orchestration.vm;

import javax.inject.Inject;

import com.godaddy.hfs.vm.VmService;
import com.godaddy.vps4.orchestration.ActionCommand;
import com.godaddy.vps4.orchestration.hfs.vm.EndRescueVm;
import com.godaddy.vps4.vm.ActionService;

import gdg.hfs.orchestration.CommandContext;
import gdg.hfs.orchestration.CommandMetadata;
import gdg.hfs.orchestration.CommandRetryStrategy;


@CommandMetadata(
        name="Vps4EndRescue",
        requestType=VmActionRequest.class,
        responseType=Void.class,
        retryStrategy = CommandRetryStrategy.NEVER
    )
public class Vps4EndRescue extends ActionCommand<VmActionRequest, Void> {

    final ActionService actionService;
    final VmService vmService;

    @Inject
    public Vps4EndRescue(ActionService actionService, VmService vmService) {
        super(actionService);
        this.actionService = actionService;
        this.vmService = vmService;
    }

    @Override
    protected Void executeWithAction(CommandContext context, VmActionRequest request) {

        context.execute(EndRescueVm.class, request.virtualMachine.hfsVmId);
        return null;
    }

}
