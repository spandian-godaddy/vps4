package com.godaddy.vps4.orchestration.vm;

import com.godaddy.hfs.vm.VmAction;
import com.godaddy.vps4.hfs.HfsVmTrackingRecordService;
import com.godaddy.vps4.orchestration.hfs.vm.WaitForVmAction;
import com.google.inject.Inject;

import gdg.hfs.orchestration.Command;
import gdg.hfs.orchestration.CommandContext;

public class WaitForAndRecordVmAction implements Command<VmAction, VmAction> {

    private final HfsVmTrackingRecordService hfsVmTrackingRecordService;

    @Inject
    public WaitForAndRecordVmAction(HfsVmTrackingRecordService hfsVmTrackingRecordService) {
        this.hfsVmTrackingRecordService = hfsVmTrackingRecordService;
    }

    @Override
    public VmAction execute(CommandContext context, VmAction hfsAction) {
        hfsAction = context.execute(WaitForVmAction.class, hfsAction);

        if (hfsAction.actionType.equals("CREATE")) {
            hfsVmTrackingRecordService.setHfsVmCreated(hfsAction.vmId);
        } else if (hfsAction.actionType.equals("DESTROY")) {
            hfsVmTrackingRecordService.setHfsVmDestroyed(hfsAction.vmId);
        }

        return hfsAction;
    }
}
