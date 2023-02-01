package com.godaddy.vps4.orchestration.snapshot;

import java.util.UUID;

import org.json.simple.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.godaddy.vps4.orchestration.ActionCommand;
import com.godaddy.vps4.orchestration.Vps4ActionRequest;
import com.godaddy.vps4.snapshot.SnapshotActionService;
import com.godaddy.vps4.snapshot.SnapshotService;
import com.godaddy.vps4.snapshot.SnapshotStatus;
import com.godaddy.vps4.vm.ActionService;
import com.godaddy.vps4.vm.ActionType;
import com.google.inject.Inject;

import gdg.hfs.orchestration.CommandContext;
import gdg.hfs.orchestration.CommandMetadata;
import gdg.hfs.orchestration.CommandRetryStrategy;

@CommandMetadata(
        name="Vps4DeprecateSnapshot",
        requestType=Vps4DeprecateSnapshot.Request.class,
        retryStrategy = CommandRetryStrategy.NEVER
)
public class Vps4DeprecateSnapshot extends ActionCommand<Vps4DeprecateSnapshot.Request, Void> {
    private static final Logger logger = LoggerFactory.getLogger(Vps4DeprecateSnapshot.class);
    private final SnapshotService snapshotService;

    private CommandContext context;
    private String initiatedBy;
    private UUID snapshotIdToDeprecate;
    private UUID vmId;

    @Inject
    public Vps4DeprecateSnapshot(@SnapshotActionService ActionService actionService, SnapshotService snapshotService) {
        super(actionService);
        this.snapshotService = snapshotService;
    }

    public static class Request extends Vps4ActionRequest {
        public UUID snapshotIdToDeprecate;
        public String initiatedBy;

        public Request() {} // needed for deserialization

        public Request(UUID vmId, UUID snapshotIdToDeprecate, String initiatedBy) {
            this.vmId = vmId;
            this.snapshotIdToDeprecate = snapshotIdToDeprecate;
            this.initiatedBy = initiatedBy;
        }
    }

    @Override
    protected Void executeWithAction(CommandContext context, Request request) {
        this.context = context;
        this.initiatedBy = request.initiatedBy;
        this.snapshotIdToDeprecate = request.snapshotIdToDeprecate;
        this.vmId = request.vmId;

        if (snapshotIdToDeprecate != null) {
            deprecateOldSnapshot();
            try {
                destroyOldSnapshot();
            } catch (Exception ignored) {
                // Squelch any exceptions because we can't really do anything about it
                logger.info("Destroy failure for snapshot with id: {}", snapshotIdToDeprecate);
            }
        }
        return null;
    }

    private void deprecateOldSnapshot() {
        logger.info("Deprecate snapshot with id: {}", snapshotIdToDeprecate);
        context.execute("MarkSnapshotAsDeprecated-" + snapshotIdToDeprecate, ctx -> {
            snapshotService.updateSnapshotStatus(snapshotIdToDeprecate, SnapshotStatus.DEPRECATED);
            return null;
        }, Void.class);
    }

    private void destroyOldSnapshot() {
        Long hfsSnapshotIdToDeprecate = snapshotService.getSnapshot(snapshotIdToDeprecate).hfsSnapshotId;
        long actionId = actionService.createAction(snapshotIdToDeprecate, ActionType.DESTROY_SNAPSHOT,
                                                   new JSONObject().toJSONString(), initiatedBy);

        Vps4DestroySnapshot.Request destroyRequest = new Vps4DestroySnapshot.Request();
        destroyRequest.hfsSnapshotId = hfsSnapshotIdToDeprecate;
        destroyRequest.vps4SnapshotId = snapshotIdToDeprecate;
        destroyRequest.actionId = actionId;
        destroyRequest.vmId = vmId;
        context.execute(Vps4DestroySnapshot.class, destroyRequest);
    }
}
