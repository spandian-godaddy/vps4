package com.godaddy.vps4.orchestration.vm;

import javax.inject.Inject;

import com.godaddy.vps4.credit.CreditService;
import com.godaddy.vps4.orchestration.hfs.vm.EndRescueVm;
import com.godaddy.vps4.vm.ActionService;

import gdg.hfs.orchestration.CommandContext;
import gdg.hfs.orchestration.CommandMetadata;
import gdg.hfs.orchestration.CommandRetryStrategy;

@CommandMetadata(
        name="Vps4AbuseSuspendDedicated",
        requestType=VmActionRequest.class,
        responseType=Void.class,
        retryStrategy = CommandRetryStrategy.NEVER
    )
public class Vps4ReinstateDedicated extends Vps4ReinstateVm {

    @Inject
    public Vps4ReinstateDedicated(ActionService actionService, CreditService creditService) {
        super(actionService, creditService);
    }

    @Override
    protected void reinstateVm(CommandContext context, VmActionRequest request) {
        context.execute(EndRescueVm.class, request.virtualMachine.hfsVmId);
    }

}
