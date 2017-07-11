package com.godaddy.vps4.orchestration.snapshot;

import com.godaddy.vps4.orchestration.ActionCommand;
import com.godaddy.vps4.orchestration.ActionRequest;
import com.godaddy.vps4.snapshot.SnapshotService;
import com.godaddy.vps4.vm.ActionService;
import gdg.hfs.orchestration.CommandContext;
import gdg.hfs.orchestration.CommandMetadata;
import gdg.hfs.vhfs.snapshot.SnapshotAction;
import gdg.hfs.vhfs.snapshot.Snapshot;

import javax.inject.Inject;
import javax.inject.Named;
import java.util.UUID;

@CommandMetadata(
        name="Vps4SnapshotVm",
        requestType=Vps4SnapshotVm.Request.class,
        responseType=Vps4SnapshotVm.Response.class
    )
public class Vps4SnapshotVm extends ActionCommand<Vps4SnapshotVm.Request, Vps4SnapshotVm.Response> {

    private final gdg.hfs.vhfs.snapshot.SnapshotService hfsSnapshotService;
    private final SnapshotService vps4SnapshotService;

    @Inject
    public Vps4SnapshotVm(@Named("Snapshot_action") ActionService actionService,
                          gdg.hfs.vhfs.snapshot.SnapshotService hfsSnapshotService,
                          SnapshotService vps4SnapshotService) {
        super(actionService);
        this.hfsSnapshotService = hfsSnapshotService;
        this.vps4SnapshotService = vps4SnapshotService;
    }

    @Override
    protected Response executeWithAction(CommandContext context, Vps4SnapshotVm.Request request) throws Exception {
        SnapshotAction hfsAction = createAndWaitForSnapshotCompletion(context, request);
        return generateResponse(hfsAction);
    }

    private Response generateResponse(SnapshotAction hfsAction) {
        Response response = new Response();
        response.hfsAction = hfsAction;
        return response;
    }

    private SnapshotAction createAndWaitForSnapshotCompletion(CommandContext context, Request request) {
        SnapshotAction hfsAction = createSnapshot(context, request);
        updateHfsSnapshotId(request.vps4SnapshotId, hfsAction.snapshotId);
        hfsAction = WaitForSnapshotCompletion(context, request, hfsAction);
        updateHfsImageId(context, request.vps4SnapshotId, hfsAction.snapshotId);
        return hfsAction;
    }

    private SnapshotAction WaitForSnapshotCompletion(CommandContext context, Request request, SnapshotAction hfsAction) {
        try {
            hfsAction = context.execute(WaitForSnapshotAction.class, hfsAction);
        } catch (Exception e) {
            vps4SnapshotService.markSnapshotErrored(request.vps4SnapshotId);
            throw new RuntimeException(e);
        }

        vps4SnapshotService.markSnapshotComplete(request.vps4SnapshotId);
        return hfsAction;
    }

    private SnapshotAction createSnapshot(CommandContext context, Request request) {
        long vmId = request.hfsVmId;
        String version = "1.0.0";

        try {
            SnapshotAction hfsAction = context.execute(
                    "Vps4SnapshotVm",
                    ctx -> hfsSnapshotService.createSnapshot(vmId, request.snapshotName, version, true, false)
            );

            vps4SnapshotService.markSnapshotInProgress(request.vps4SnapshotId);
            return hfsAction;
        } catch (Exception e) {
            vps4SnapshotService.markSnapshotErrored(request.vps4SnapshotId);
            throw new RuntimeException(e);
        }
    }

    private void updateHfsSnapshotId(UUID vps4SnapshotId, long hfsSnapshotId) {
        vps4SnapshotService.updateHfsSnapshotId(vps4SnapshotId, hfsSnapshotId);
    }

    private void updateHfsImageId(CommandContext context, UUID vps4SnapshotId, long hfsSnapshotId) {
        Snapshot hfsSnapshot = context.execute(
                "GetHFSSnapshot",
                ctx -> hfsSnapshotService.getSnapshot(hfsSnapshotId)
        );

        vps4SnapshotService.updateHfsImageId(vps4SnapshotId, hfsSnapshot.imageId);
    }

    public static class Request implements ActionRequest {
        public long actionId;
        public long hfsVmId;
        public String snapshotName;
        public UUID vps4SnapshotId;
        public UUID vps4VmId;

        @Override
        public long getActionId() {
            return actionId;
        }
    }

    public static class Response {
        public SnapshotAction hfsAction;
    }

}