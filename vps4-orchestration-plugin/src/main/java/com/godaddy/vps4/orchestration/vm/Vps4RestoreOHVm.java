package com.godaddy.vps4.orchestration.vm;

import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.godaddy.hfs.vm.VmAction;
import com.godaddy.vps4.orchestration.ActionCommand;
import com.godaddy.vps4.orchestration.hfs.vm.RestoreOHVm;
import com.godaddy.vps4.snapshot.SnapshotService;
import com.godaddy.vps4.vm.ActionService;
import com.google.inject.Inject;

import gdg.hfs.orchestration.CommandContext;
import gdg.hfs.orchestration.CommandMetadata;
import gdg.hfs.orchestration.CommandRetryStrategy;

@CommandMetadata(
        name = "Vps4RestoreOHVm",
        requestType = Vps4RestoreOHVm.Request.class,
        responseType = Vps4RestoreOHVm.Response.class,
        retryStrategy = CommandRetryStrategy.NEVER
)
public class Vps4RestoreOHVm extends ActionCommand<Vps4RestoreOHVm.Request, Vps4RestoreOHVm.Response> {

    private static final Logger logger = LoggerFactory.getLogger(Vps4RestoreOHVm.class);
    private final SnapshotService vps4SnapshotService;
    private Request request;

    @Inject
    public Vps4RestoreOHVm(ActionService actionService, SnapshotService vps4SnapshotService) {
        super(actionService);
        this.vps4SnapshotService = vps4SnapshotService;
    }

    @Override
    public Response executeWithAction(CommandContext context, Request request) {
        this.request = request;

        long hfsSnapshotId = context.execute("GetHfsSnapshotId",
                                               ctx -> vps4SnapshotService.getSnapshot(request.vps4SnapshotId).hfsSnapshotId,
                                               long.class);

        RestoreOHVm.Request restoreOHVmRequest= new RestoreOHVm.Request(request.virtualMachine.hfsVmId, hfsSnapshotId);
        VmAction hfsAction = context.execute(RestoreOHVm.class, restoreOHVmRequest);

        Vps4RestoreOHVm.Response response = new Vps4RestoreOHVm.Response(request.virtualMachine.hfsVmId,
                                                                         hfsSnapshotId, hfsAction.vmActionId);

        return response;
    }

    public static class Request extends VmActionRequest{
        public UUID vps4SnapshotId;

        public Request(){}

        public Request(UUID snapshotId){
            this.vps4SnapshotId = snapshotId;
        }
    }

    public static class Response {
        public long hfsVmId;
        public long hfsSnapshotId;
        public long hfsActionId;

        public Response(){}

        public Response(long hfsVmId, long hfsSnapshotId, long hfsActionId) {
            this.hfsVmId = hfsVmId;
            this.hfsSnapshotId = hfsSnapshotId;
            this.hfsActionId = hfsActionId;
        }
    }
}
