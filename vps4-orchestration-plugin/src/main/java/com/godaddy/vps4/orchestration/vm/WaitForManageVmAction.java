package com.godaddy.vps4.orchestration.vm;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.godaddy.vps4.Vps4Exception;

import gdg.hfs.orchestration.Command;
import gdg.hfs.orchestration.CommandContext;
import gdg.hfs.vhfs.vm.VmAction;
import gdg.hfs.vhfs.vm.VmService;

public class WaitForManageVmAction implements Command<VmAction, VmAction> {

    private static final Logger logger = LoggerFactory.getLogger(WaitForManageVmAction.class);

    private final VmService vmService;

    @Inject
    public WaitForManageVmAction(VmService vmService) {
        this.vmService = vmService;
    }

    @Override
    public VmAction execute(CommandContext context, VmAction hfsAction) {
        // wait for VmAction to complete
        while (hfsAction.state == VmAction.Status.NEW 
                || hfsAction.state == VmAction.Status.REQUESTED 
                || hfsAction.state == VmAction.Status.IN_PROGRESS) {

            logger.info("waiting for vm action to complete: {}", hfsAction);         

            // sleep for 2 secs - allow vm action to complete on hfs side.
            try {
                Thread.sleep(2000);
            }
            catch (InterruptedException e) {
                logger.warn("Interrupted while sleeping");
            }

            hfsAction = vmService.getVmAction(hfsAction.vmId, hfsAction.vmActionId);
        }
        if(hfsAction.state == VmAction.Status.COMPLETE) {
            logger.info("Vm Action completed. hfsAction: {} ", hfsAction );
        } else {
            throw new Vps4Exception("Manage_VM_Failed", String.format(" Failed action: %s", hfsAction));
        } 
        return hfsAction;
    }

}
