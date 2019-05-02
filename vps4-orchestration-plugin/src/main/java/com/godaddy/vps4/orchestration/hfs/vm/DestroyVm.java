package com.godaddy.vps4.orchestration.hfs.vm;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.godaddy.hfs.vm.VmAction;
import com.godaddy.hfs.vm.VmService;
import com.godaddy.vps4.hfs.HfsVmTrackingRecordService;
import com.godaddy.vps4.orchestration.vm.WaitForAndRecordVmAction;

import gdg.hfs.orchestration.Command;
import gdg.hfs.orchestration.CommandContext;

public class DestroyVm implements Command<Long, VmAction> {

    private static final Logger logger = LoggerFactory.getLogger(DestroyVm.class);
    final VmService vmService;
    final HfsVmTrackingRecordService hfsTrackingService;

    @Inject
    public DestroyVm(VmService vmService, HfsVmTrackingRecordService hfsTrackingService) {
        this.vmService = vmService;
        this.hfsTrackingService = hfsTrackingService;
    }

    @Override
    public VmAction execute(CommandContext context, Long hfsVmId) {

        if (hfsVmId == 0 || isAlreadyDestroyed(hfsVmId)) {
            logger.info("Skipping deletion of HFS VM {} - already deleted or non-existent", hfsVmId);
            return null;
        }

        VmAction hfsAction = context.execute("RequestDestroy", ctx -> vmService.destroyVm(hfsVmId), VmAction.class);
        hfsTrackingService.setCanceled(hfsVmId);

        context.execute(WaitForAndRecordVmAction.class, hfsAction);

        return hfsAction;
    }

    private boolean isAlreadyDestroyed(long hfsVmId) {
        return vmService.getVm(hfsVmId).status.equals("DESTROYED");
    }

}
