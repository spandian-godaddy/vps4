package com.godaddy.vps4.orchestration.vm;

import javax.inject.Inject;

import com.godaddy.vps4.credit.CreditService;
import com.godaddy.vps4.orchestration.hfs.vm.RescueVm;
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
public class Vps4AbuseSuspendDedicated extends Vps4AbuseSuspendVm {

    @Inject
    public Vps4AbuseSuspendDedicated(ActionService actionService, CreditService creditService) {
        super(actionService, creditService);
    }

    @Override
    protected void suspendVm(CommandContext context, VmActionRequest request) {
        context.execute(RescueVm.class, request.virtualMachine.hfsVmId);
    }

}
