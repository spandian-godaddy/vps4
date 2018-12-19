package com.godaddy.vps4.orchestration.hfs.vm;

import javax.inject.Inject;

import com.godaddy.hfs.vm.VmAction;
import com.godaddy.hfs.vm.VmService;
import gdg.hfs.orchestration.Command;
import gdg.hfs.orchestration.CommandContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RebuildDedicated implements Command<RebuildDedicated.Request, VmAction> {

    private static final Logger logger = LoggerFactory.getLogger(RebuildDedicated.class);
    private final VmService vmService;

    @Inject
    public RebuildDedicated(VmService vmService) {
        this.vmService = vmService;
    }

    @Override
    public VmAction execute(CommandContext context, RebuildDedicated.Request request) {
        logger.info("sending HFS VM request: {}", request);

        VmAction vmAction = context.execute("RebuildDedicated", ctx -> vmService.rebuildVm(request.vmId), VmAction.class);

        context.execute(WaitForVmAction.class, vmAction);

        return vmAction;
    }

    public static class Request {
        public long vmId;
    }
}
