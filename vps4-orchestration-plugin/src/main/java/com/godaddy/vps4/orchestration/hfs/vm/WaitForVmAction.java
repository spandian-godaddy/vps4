package com.godaddy.vps4.orchestration.hfs.vm;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import gdg.hfs.orchestration.Command;
import gdg.hfs.orchestration.CommandContext;
import com.godaddy.hfs.vm.VmAction;
import com.godaddy.hfs.vm.VmService;
import com.godaddy.vps4.orchestration.scheduler.Utils;

public class WaitForVmAction implements Command<VmAction, Void> {

    private static final Logger logger = LoggerFactory.getLogger(WaitForVmAction.class);

    private final VmService vmService;

    @Inject
    public WaitForVmAction(VmService vmService) {
        this.vmService = vmService;
    }

    @Override
    public Void execute(CommandContext context, VmAction hfsAction) {

        hfsAction = vmService.getVmAction(hfsAction.vmId, hfsAction.vmActionId);

        int currentHfsTick = 1;

        long vmId = hfsAction.vmId;
        long vmActionId = hfsAction.vmActionId;

        // wait for VmAction to complete
        while (hfsAction.state == VmAction.Status.NEW
                || hfsAction.state == VmAction.Status.REQUESTED
                || hfsAction.state == VmAction.Status.IN_PROGRESS) {

            logger.debug("waiting for action {} to complete, current state: {}", hfsAction.vmActionId, hfsAction.state);

            if (hfsAction.tickNum > currentHfsTick) {
                currentHfsTick = hfsAction.tickNum;
            }

            hfsAction = Utils.runWithRetriesForServerErrorException(context, logger, () ->{
                return vmService.getVmAction(vmId, vmActionId);
            });
        }
        if (!(hfsAction.state == VmAction.Status.COMPLETE)) {
            throw new RuntimeException(String.format("failed to complete VM action: %s", hfsAction));
        }
        return null;
    }
}
