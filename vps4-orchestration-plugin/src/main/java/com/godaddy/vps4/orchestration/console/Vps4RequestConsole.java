package com.godaddy.vps4.orchestration.console;

import java.util.UUID;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.godaddy.hfs.vm.ConsoleRequest;
import com.godaddy.hfs.vm.VmAction;
import com.godaddy.hfs.vm.VmService;
import com.godaddy.vps4.orchestration.ActionCommand;
import com.godaddy.vps4.orchestration.Vps4ActionRequest;
import com.godaddy.vps4.orchestration.hfs.vm.WaitForVmAction;
import com.godaddy.vps4.vm.ActionService;

import gdg.hfs.orchestration.CommandContext;
import gdg.hfs.orchestration.CommandMetadata;
import gdg.hfs.orchestration.CommandRetryStrategy;

@CommandMetadata(
        name = "Vps4RequestConsole",
        requestType = Vps4RequestConsole.Request.class,
        retryStrategy = CommandRetryStrategy.NEVER
)
public class Vps4RequestConsole extends ActionCommand<Vps4RequestConsole.Request, Void> {
    private static final Logger logger = LoggerFactory.getLogger(Vps4RequestConsole.class);
    private final VmService vmService;

    @Inject
    public Vps4RequestConsole(ActionService actionService, VmService vmService) {
        super(actionService);
        this.vmService = vmService;
    }

    public static class Request extends Vps4ActionRequest {
        public UUID vmId;
        public long hfsVmId;
        public String fromIpAddress;
    }

    @Override
    public Void executeWithAction(CommandContext context, Request request) {
        logger.info("Asking HFS for a console URL for vmId: {}, hfsVmId: {}", request.vmId, request.hfsVmId);

        ConsoleRequest hfsRequest = new ConsoleRequest();
        hfsRequest.allowedAddress = request.fromIpAddress;
        VmAction hfsAction = context.execute("RequestConsoleHfs",
                                             ctx -> vmService.createConsoleUrl(request.hfsVmId, hfsRequest),
                                             VmAction.class);
        context.execute(WaitForVmAction.class, hfsAction);

        return null;
    }
}
