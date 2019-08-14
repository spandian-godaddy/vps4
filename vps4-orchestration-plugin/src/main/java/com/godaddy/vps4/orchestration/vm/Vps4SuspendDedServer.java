package com.godaddy.vps4.orchestration.vm;

import javax.inject.Inject;

import com.godaddy.hfs.config.Config;
import com.godaddy.vps4.credit.CreditService;
import com.godaddy.vps4.orchestration.hfs.vm.RescueVm;
import com.godaddy.vps4.vm.ActionService;

import gdg.hfs.orchestration.CommandContext;
import gdg.hfs.orchestration.CommandMetadata;
import gdg.hfs.orchestration.CommandRetryStrategy;

@CommandMetadata(
        name="Vps4SuspendDedServer",
        requestType=Vps4SuspendServer.Request.class,
        responseType=Void.class,
        retryStrategy = CommandRetryStrategy.NEVER
    )
public class Vps4SuspendDedServer extends Vps4SuspendServer {

    @Inject
    public Vps4SuspendDedServer(ActionService actionService, CreditService creditService, Config config) {
        super(actionService, creditService, config);
    }

    @Override
    protected void suspendVm(CommandContext context, Vps4SuspendServer.Request request) {
        context.execute(RescueVm.class, request.virtualMachine.hfsVmId);
    }

}
