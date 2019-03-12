package com.godaddy.vps4.orchestration.vm;

import javax.inject.Inject;

import com.godaddy.hfs.vm.VmAction;
import com.godaddy.hfs.vm.VmService;
import com.godaddy.vps4.credit.CreditService;
import com.godaddy.vps4.vm.ActionService;

import gdg.hfs.orchestration.CommandContext;
import gdg.hfs.orchestration.CommandMetadata;
import gdg.hfs.orchestration.CommandRetryStrategy;

@CommandMetadata(
        name="Vps4AbuseSuspendDedicated",
        requestType=VmActionRequest.class,
        responseType= Vps4AbuseSuspendVm.Response.class,
        retryStrategy = CommandRetryStrategy.NEVER
    )
public class Vps4AbuseSuspendDedicated extends Vps4AbuseSuspendVm {

    @Inject
    public Vps4AbuseSuspendDedicated(ActionService actionService, VmService vmService, CreditService creditService) {
        super(actionService, vmService, creditService);
    }

    @Override
    protected VmAction suspendVm(CommandContext context, VmActionRequest request) {
        VmAction hfsAction = context.execute("Vps4RescueVm", ctx -> vmService.rescueVm(request.virtualMachine.hfsVmId), VmAction.class);
        hfsAction = context.execute(WaitForManageVmAction.class, hfsAction);
        return hfsAction;
    }

}
