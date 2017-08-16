package com.godaddy.vps4.orchestration.snapshot;

import java.util.UUID;

import javax.inject.Inject;

import org.apache.commons.lang3.RandomStringUtils;
import org.json.simple.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.godaddy.vps4.orchestration.ActionCommand;
import com.godaddy.vps4.orchestration.ActionRequest;
import com.godaddy.vps4.snapshot.Snapshot;
import com.godaddy.vps4.snapshot.SnapshotActionService;
import com.godaddy.vps4.snapshot.SnapshotService;
import com.godaddy.vps4.snapshot.SnapshotType;
import com.godaddy.vps4.vm.ActionService;
import com.godaddy.vps4.vm.ActionType;

import gdg.hfs.orchestration.CommandContext;
import gdg.hfs.orchestration.CommandMetadata;
import gdg.hfs.vhfs.snapshot.SnapshotAction;

@CommandMetadata(
        name="Vps4SnapshotVm",
        requestType=Vps4SnapshotVm.Request.class,
        responseType=Vps4SnapshotVm.Response.class
    )
public class Vps4SnapshotVm extends ActionCommand<Vps4SnapshotVm.Request, Vps4SnapshotVm.Response> {
    private static final Logger logger = LoggerFactory.getLogger(Vps4SnapshotVm.class);

    private final gdg.hfs.vhfs.snapshot.SnapshotService hfsSnapshotService;
    private final SnapshotService vps4SnapshotService;
    private UUID snapshotIdToBeDeprecated;

    @Inject
    public Vps4SnapshotVm(@SnapshotActionService ActionService actionService,
                          gdg.hfs.vhfs.snapshot.SnapshotService hfsSnapshotService,
                          SnapshotService vps4SnapshotService) {
        super(actionService);
        this.hfsSnapshotService = hfsSnapshotService;
        this.vps4SnapshotService = vps4SnapshotService;
    }

    @Override
    protected Response executeWithAction(CommandContext context, Vps4SnapshotVm.Request request) throws Exception {
        snapshotIdToBeDeprecated = vps4SnapshotService.markOldestSnapshotForDeprecation(request.orionGuid, request.snapshotType);
        SnapshotAction hfsAction = createAndWaitForSnapshotCompletion(context, request);
        deprecateOldSnapshot(context, request.vps4UserId);
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

    private void deprecateOldSnapshot(CommandContext context, long vps4UserId) {
        if (snapshotIdToBeDeprecated != null) {
            logger.info("Deprecate snapshot with id: {}", snapshotIdToBeDeprecated);
            vps4SnapshotService.markSnapshotAsDeprecated(snapshotIdToBeDeprecated);
            Snapshot vps4Snapshot = vps4SnapshotService.getSnapshot(snapshotIdToBeDeprecated);

            try {
                // now destroy the deprecated snapshot
                destroyOldSnapshot(context, vps4UserId, vps4Snapshot.hfsSnapshotId);
            } catch (Exception e) {
                // Squelch any exceptions because we cant really do anything about it?
                logger.info("Deprecation/Destroy failure for snapshot with id: {}", snapshotIdToBeDeprecated);
            }
        }
    }

    private void destroyOldSnapshot(CommandContext context, long vps4UserId, long hfsSnapshotId) {
        long delActionId = actionService.createAction(
                snapshotIdToBeDeprecated,  ActionType.DESTROY_SNAPSHOT, new JSONObject().toJSONString(), vps4UserId);

        Vps4DestroySnapshot.Request req = new Vps4DestroySnapshot.Request();
        req.hfsSnapshotId = hfsSnapshotId;
        req.vps4SnapshotId = snapshotIdToBeDeprecated;
        req.actionId = delActionId;
        context.execute(Vps4DestroySnapshot.class, req);
    }

    private SnapshotAction WaitForSnapshotCompletion(CommandContext context, Request request,
                                                     SnapshotAction hfsAction) {
        try {
            hfsAction = context.execute(WaitForSnapshotAction.class, hfsAction);
        } catch (Exception e) {
            logger.info("Snapshot creation error (waitForAction) for snapshot with id: {}", request.vps4SnapshotId);
            vps4SnapshotService.markSnapshotErrored(request.vps4SnapshotId);
            reverseSnapshotDeprecation();
            throw new RuntimeException(e);
        }

        vps4SnapshotService.markSnapshotLive(request.vps4SnapshotId);
        return hfsAction;
    }

    private SnapshotAction createSnapshot(CommandContext context, Request request) {
        long vmId = request.hfsVmId;
        String version = "1.0.0";
        //  random snapshot name to hfs layer
        String name = String.format("vps4-%s-%s",
                request.snapshotType.name().toLowerCase().substring(0, 4),
                RandomStringUtils.randomAlphabetic(8));

        try {
            SnapshotAction hfsAction = context.execute(
                    "Vps4SnapshotVm",
                    ctx -> hfsSnapshotService.createSnapshot(vmId, name, version, true, false)
            );

            logger.info("HFS snapshot create request returned action: {}", hfsAction);
            vps4SnapshotService.markSnapshotInProgress(request.vps4SnapshotId);
            return hfsAction;
        } catch (Exception e) {
            logger.info("Snapshot creation error for VPS4 snapshot with id: {}", request.vps4SnapshotId);
            vps4SnapshotService.markSnapshotErrored(request.vps4SnapshotId);
            reverseSnapshotDeprecation();
            throw new RuntimeException(e);
        }
    }

    private void reverseSnapshotDeprecation() {
        if (snapshotIdToBeDeprecated != null)
            logger.info("Reverse deprecation of VPS4 snapshot with id: {}", snapshotIdToBeDeprecated);
            vps4SnapshotService.reverseSnapshotDeprecation(snapshotIdToBeDeprecated);
    }

    private void updateHfsSnapshotId(UUID vps4SnapshotId, long hfsSnapshotId) {
        logger.info("Update VPS4 snapshot [{}] with HFS snapshot id: {}", vps4SnapshotId, hfsSnapshotId);
        vps4SnapshotService.updateHfsSnapshotId(vps4SnapshotId, hfsSnapshotId);
    }

    private void updateHfsImageId(CommandContext context, UUID vps4SnapshotId, long hfsSnapshotId) {
        gdg.hfs.vhfs.snapshot.Snapshot hfsSnapshot = context.execute(
                "GetHFSSnapshot",
                ctx -> hfsSnapshotService.getSnapshot(hfsSnapshotId)
        );

        logger.info("Update VPS4 snapshot [{}] with Nocfox image id: {}", vps4SnapshotId, hfsSnapshot.imageId);
        vps4SnapshotService.updateHfsImageId(vps4SnapshotId, hfsSnapshot.imageId);
    }

    public static class Request implements ActionRequest {
        public long actionId;
        public long hfsVmId;
        public UUID vps4SnapshotId;
        public UUID orionGuid;
        public long vps4UserId;
        public SnapshotType snapshotType;

        @Override
        public long getActionId() {
            return actionId;
        }
    }

    public static class Response {
        public SnapshotAction hfsAction;
    }

}