package com.godaddy.vps4.orchestration.vm;

import javax.inject.Inject;

import com.godaddy.hfs.config.Config;
import com.godaddy.vps4.credit.CreditService;
import com.godaddy.vps4.orchestration.hfs.vm.EndRescueVm;
import com.godaddy.vps4.vm.ActionService;

import gdg.hfs.orchestration.CommandContext;
import gdg.hfs.orchestration.CommandMetadata;
import gdg.hfs.orchestration.CommandRetryStrategy;

@CommandMetadata(
        name="Vps4ReinstateDedServer",
        requestType=Vps4ReinstateServer.Request.class,
        responseType=Void.class,
        retryStrategy = CommandRetryStrategy.NEVER
    )
public class Vps4ReinstateDedServer extends Vps4ReinstateServer {

    @Inject
    public Vps4ReinstateDedServer(ActionService actionService, CreditService creditService, Config config) {
        super(actionService, creditService, config);
    }

    @Override
    protected void reinstateVm(CommandContext context, Vps4ReinstateServer.Request request) {
        context.execute(EndRescueVm.class, request.virtualMachine.hfsVmId);
    }

}
