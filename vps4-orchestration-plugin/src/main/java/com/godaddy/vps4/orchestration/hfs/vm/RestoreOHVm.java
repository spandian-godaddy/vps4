package com.godaddy.vps4.orchestration.hfs.vm;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.godaddy.hfs.vm.VmAction;
import com.godaddy.hfs.vm.VmService;

import gdg.hfs.orchestration.Command;
import gdg.hfs.orchestration.CommandContext;

public class RestoreOHVm implements Command<RestoreOHVm.Request, VmAction> {

    private static final Logger logger = LoggerFactory.getLogger(RestoreOHVm.class);
    private final VmService hfsVmService;

    @Inject
    public RestoreOHVm(VmService vmService) {
        this.hfsVmService = vmService;
    }

    @Override
    public VmAction execute(CommandContext context, Request request) {
        VmAction action = context.execute("RestoreOHVm", ctx -> hfsVmService.restore(request.hfsVmId, request.hfsSnapshotId), VmAction.class);
        context.execute(WaitForVmAction.class, action);
        return action;
    }

    public static class Request {
        public long hfsVmId;
        public long hfsSnapshotId;

        public Request(){}

        public Request(long hfsVmId, long hfsSnapshotId){
            this.hfsVmId = hfsVmId;
            this.hfsSnapshotId = hfsSnapshotId;
        }
    }

}
