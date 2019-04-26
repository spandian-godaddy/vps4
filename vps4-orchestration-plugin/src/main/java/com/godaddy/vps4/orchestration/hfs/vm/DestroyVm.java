package com.godaddy.vps4.orchestration.hfs.vm;

import javax.inject.Inject;

import gdg.hfs.orchestration.Command;
import gdg.hfs.orchestration.CommandContext;
import com.godaddy.hfs.vm.VmAction;
import com.godaddy.hfs.vm.VmService;
import com.godaddy.vps4.hfs.HfsVmTrackingRecordService;
import com.godaddy.vps4.orchestration.vm.WaitForAndRecordVmAction;

public class DestroyVm implements Command<Long, VmAction> {

    final VmService vmService;
    final HfsVmTrackingRecordService hfsVmService;

    @Inject
    public DestroyVm(VmService vmService, HfsVmTrackingRecordService hfsVmService) {
        this.vmService = vmService;
        this.hfsVmService = hfsVmService;
    }

    @Override
    public VmAction execute(CommandContext context, Long vmId) {

        VmAction hfsAction = context.execute("RequestDestroy", ctx -> vmService.destroyVm(vmId), VmAction.class);
        hfsVmService.setCanceled(vmId);

        context.execute(WaitForAndRecordVmAction.class, hfsAction);

        return hfsAction;
    }


}
