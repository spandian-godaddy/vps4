package com.godaddy.vps4.orchestration.hfs.vm;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.godaddy.hfs.vm.Vm;
import com.godaddy.hfs.vm.VmAction;
import com.godaddy.hfs.vm.VmService;
import com.godaddy.vps4.hfs.HfsVmTrackingRecordService;
import com.godaddy.vps4.orchestration.ActionRequest;
import com.godaddy.vps4.orchestration.vm.WaitForAndRecordVmAction;

import gdg.hfs.orchestration.Command;
import gdg.hfs.orchestration.CommandContext;

public class DestroyVm implements Command<DestroyVm.Request, VmAction> {

    private static final Logger logger = LoggerFactory.getLogger(DestroyVm.class);
    final VmService vmService;
    private final HfsVmTrackingRecordService hfsVmTrackingRecordService;

    @Inject
    public DestroyVm(VmService vmService, HfsVmTrackingRecordService hfsVmTrackingRecordService) {
        this.vmService = vmService;
        this.hfsVmTrackingRecordService = hfsVmTrackingRecordService;
    }

    @Override
    public VmAction execute(CommandContext context, DestroyVm.Request request) {

        long hfsVmId = request.hfsVmId;

        if (shouldSkipHfsDestroy(hfsVmId)) {
            logger.info("Skipping deletion of HFS VM {} - already deleted or non-existent", hfsVmId);
            return null;
        }

        VmAction hfsAction = context.execute("RequestDestroy", ctx -> vmService.destroyVm(hfsVmId), VmAction.class);

        context.execute(WaitForAndRecordVmAction.class, hfsAction);

        updateHfsVmTrackingRecord(context, hfsVmId, request.actionId);

        return hfsAction;
    }

    public void updateHfsVmTrackingRecord(CommandContext context, long hfsVmId, long vps4ActionId){
        context.execute("UpdateHfsVmTrackingRecord", ctx -> {
            hfsVmTrackingRecordService.setDestroyed(hfsVmId, vps4ActionId);
            return null;
        }, Void.class);
    }

    private boolean shouldSkipHfsDestroy(long hfsVmId) {
        if (hfsVmId == 0)
            return true;
        Vm hfsVm = vmService.getVm(hfsVmId);
        return isAlreadyDestroyed(hfsVm.status) || hasNoResourceId(hfsVm.resourceId);
    }

    private boolean isAlreadyDestroyed(String hfsStatus) {
       return hfsStatus.equals("DESTROYED");
    }

    private boolean hasNoResourceId(String hfsResourceId) {
        return hfsResourceId == null;
    }

    public static class Request implements ActionRequest {
        public long actionId;
        public long hfsVmId;

        @Override
        public long getActionId() {
            return actionId;
        }

        @Override
        public void setActionId(long actionId) {
            this.actionId = actionId;
        }

    }
}
