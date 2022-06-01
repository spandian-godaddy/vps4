package com.godaddy.vps4.orchestration.snapshot;

import java.util.UUID;

import javax.inject.Inject;

import org.apache.commons.text.RandomStringGenerator;
import org.json.simple.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.godaddy.hfs.config.Config;
import com.godaddy.vps4.orchestration.ActionCommand;
import com.godaddy.vps4.orchestration.Vps4ActionRequest;
import com.godaddy.vps4.orchestration.scheduler.ScheduleAutomaticBackupRetry;
import com.godaddy.vps4.orchestration.vm.Vps4RecordScheduledJobForVm;
import com.godaddy.vps4.scheduledJob.ScheduledJob;
import com.godaddy.vps4.snapshot.Snapshot;
import com.godaddy.vps4.snapshot.SnapshotActionService;
import com.godaddy.vps4.snapshot.SnapshotService;
import com.godaddy.vps4.snapshot.SnapshotStatus;
import com.godaddy.vps4.snapshot.SnapshotType;
import com.godaddy.vps4.vm.ActionService;
import com.godaddy.vps4.vm.ActionType;

import gdg.hfs.orchestration.CommandContext;
import gdg.hfs.orchestration.CommandMetadata;
import gdg.hfs.orchestration.CommandRetryStrategy;
import gdg.hfs.vhfs.snapshot.SnapshotAction;

@CommandMetadata(
        name="Vps4SnapshotVm",
        requestType=Vps4SnapshotVm.Request.class,
        responseType=Vps4SnapshotVm.Response.class,
        retryStrategy = CommandRetryStrategy.NEVER
)
public class Vps4SnapshotVm extends ActionCommand<Vps4SnapshotVm.Request, Vps4SnapshotVm.Response> {
    private static final Logger logger = LoggerFactory.getLogger(Vps4SnapshotVm.class);
    private CommandContext context;

    private final gdg.hfs.vhfs.snapshot.SnapshotService hfsSnapshotService;
    private final SnapshotService vps4SnapshotService;
    private UUID snapshotIdToBeDeprecated;
    private final Config config;

    @Inject
    public Vps4SnapshotVm(@SnapshotActionService ActionService actionService,
                          gdg.hfs.vhfs.snapshot.SnapshotService hfsSnapshotService,
                          SnapshotService vps4SnapshotService,
                          Config config) {
        super(actionService);
        this.hfsSnapshotService = hfsSnapshotService;
        this.vps4SnapshotService = vps4SnapshotService;
        this.config = config;
    }

    @Override
    protected Response executeWithAction(CommandContext context, Request request) {
        this.context = context;
        context.execute("CancelErroredSnapshots", ctx -> {
            vps4SnapshotService.cancelErroredSnapshots(request.orionGuid, request.snapshotType);
            return null;
        }, Void.class);
        snapshotIdToBeDeprecated = context.execute("MarkOldestSnapshotForDeprecation" + request.orionGuid,
                ctx -> vps4SnapshotService.markOldestSnapshotForDeprecation(request.orionGuid, request.snapshotType),
                UUID.class);
        SnapshotAction hfsAction = createAndWaitForSnapshotCompletion(request);
        deleteVmHvForSnapshotTracking(request.vmId);
        deprecateOldSnapshot(request.initiatedBy);
        return generateResponse(hfsAction);
    }

    private Response generateResponse(SnapshotAction hfsAction) {
        Response response = new Response();
        response.hfsAction = hfsAction;
        return response;
    }

    private SnapshotAction createAndWaitForSnapshotCompletion(Request request) {
        SnapshotAction hfsAction = createSnapshot(request);
        updateHfsSnapshotId(request.vps4SnapshotId, hfsAction.snapshotId);
        hfsAction = waitForSnapshotCompletion(request, hfsAction);
        updateHfsImageId(request.vps4SnapshotId, hfsAction.snapshotId);
        return hfsAction;
    }

    private void deprecateOldSnapshot(String initiatedBy) {
        if (snapshotIdToBeDeprecated != null) {
            logger.info("Deprecate snapshot with id: {}", snapshotIdToBeDeprecated);
            context.execute("MarkSnapshotAsDeprecated" + snapshotIdToBeDeprecated, ctx -> {
                vps4SnapshotService.updateSnapshotStatus(snapshotIdToBeDeprecated, SnapshotStatus.DEPRECATED);
                return null;
            }, Void.class);
            Snapshot vps4Snapshot = vps4SnapshotService.getSnapshot(snapshotIdToBeDeprecated);

            try {
                // now destroy the deprecated snapshot
                destroyOldSnapshot(vps4Snapshot.hfsSnapshotId, initiatedBy);
            } catch (Exception e) {
                // Squelch any exceptions because we cant really do anything about it?
                logger.info("Deprecation/Destroy failure for snapshot with id: {}", snapshotIdToBeDeprecated);
            }
        }
    }

    private void destroyOldSnapshot(long hfsSnapshotId, String initiatedBy) {
        long delActionId = actionService.createAction(
                snapshotIdToBeDeprecated,  ActionType.DESTROY_SNAPSHOT, new JSONObject().toJSONString(), initiatedBy);

        Vps4DestroySnapshot.Request req = new Vps4DestroySnapshot.Request();
        req.hfsSnapshotId = hfsSnapshotId;
        req.vps4SnapshotId = snapshotIdToBeDeprecated;
        req.actionId = delActionId;
        context.execute(Vps4DestroySnapshot.class, req);
    }

    private SnapshotAction waitForSnapshotCompletion(Request request, SnapshotAction hfsAction) {
        try {
            hfsAction = context.execute(WaitForSnapshotAction.class, hfsAction);
        } catch (Exception e) {
            handleSnapshotCreationError(request, e);
        }

        context.execute("MarkSnapshotLive" + request.vps4SnapshotId, ctx -> {
            vps4SnapshotService.updateSnapshotStatus(request.vps4SnapshotId, SnapshotStatus.LIVE);
            return null;
        }, Void.class);
        return hfsAction;
    }

    private boolean shouldRetryAgain(UUID vmId) {
        int numOfFailedSnapshots = vps4SnapshotService.failedBackupsSinceSuccess(vmId, SnapshotType.AUTOMATIC);
        int retryLimit = Integer.parseInt(config.get("vps4.autobackup.failedBackupRetryLimit"));
        return numOfFailedSnapshots <= retryLimit;
    }

    private void handleSnapshotCreationError(Request request, Exception e) {
        logger.info("Snapshot creation error for VPS4 snapshot with id: {}", request.vps4SnapshotId);
        vps4SnapshotService.updateSnapshotStatus(request.vps4SnapshotId, SnapshotStatus.ERROR);
        reverseSnapshotDeprecation();
        deleteVmHvForSnapshotTracking(request.vmId);
        if (request.snapshotType.equals(SnapshotType.AUTOMATIC)) {
            if (request.allowRetries && shouldRetryAgain(request.vmId)) {
                int minutesToWait = Integer.parseInt(config.get("vps4.autobackup.rescheduleFailedBackupWaitMinutes", "720"));
                rescheduleSnapshot(request, minutesToWait);
                vps4SnapshotService.updateSnapshotStatus(request.vps4SnapshotId, SnapshotStatus.ERROR_RESCHEDULED);
            } else {
                logger.warn("Retries not allowed or max retries exceeded for automatic snapshot on vm: {}  Will not retry again.",
                            request.vmId);
            }
        }
        throw new RuntimeException("Exception while running backup for vmId " + request.vmId, e);
    }

    private void rescheduleSnapshot(Request request, int minutesToWait) {
        // If an automatic snapshot fails, schedule another one in a configurable number of hours
        ScheduleAutomaticBackupRetry.Request retryRequest = new ScheduleAutomaticBackupRetry.Request();
        retryRequest.vmId = request.vmId;
        retryRequest.shopperId = request.shopperId;
        retryRequest.minutesToWait = minutesToWait;

        UUID retryJobId = context.execute(ScheduleAutomaticBackupRetry.class, retryRequest);
        recordJobId(request.vmId, retryJobId);
        logger.info("Rescheduled automatic snapshot for vm {} with retry job id: {}", request.vmId, retryJobId);
    }

    private void recordJobId(UUID vmId, UUID jobId) {
        Vps4RecordScheduledJobForVm.Request req = new Vps4RecordScheduledJobForVm.Request();
        req.jobId = jobId;
        req.vmId = vmId;
        req.jobType = ScheduledJob.ScheduledJobType.BACKUPS_RETRY;
        context.execute("RecordScheduledJobId", Vps4RecordScheduledJobForVm.class, req);
    }

    private SnapshotAction createSnapshot(Request request) {
        long vmId = request.hfsVmId;
        String version = "1.0.0";
        //  random snapshot name to hfs layer
        RandomStringGenerator generator = new RandomStringGenerator.Builder().withinRange('A', 'z').build();
        String name = String.format("vps4-%s-%s",
                request.snapshotType.name().toLowerCase().substring(0, 4),
                generator.generate(8));

        try {
            SnapshotAction hfsAction = context.execute(
                    "Vps4SnapshotVm",
                    ctx -> hfsSnapshotService.createSnapshot(vmId, name, version, true, false),
                    SnapshotAction.class
            );

            logger.info("HFS snapshot create request returned action: {}", hfsAction);
            context.execute("MarkSnapshotInProgress" + request.vps4SnapshotId, ctx -> {
                vps4SnapshotService.updateSnapshotStatus(request.vps4SnapshotId, SnapshotStatus.IN_PROGRESS);
                return null;
            }, Void.class);
            return hfsAction;
        } catch (Exception e) {
            handleSnapshotCreationError(request, e);
            return null;
        }
    }

    private void reverseSnapshotDeprecation() {
        if (snapshotIdToBeDeprecated != null)
        {
            logger.info("Reverse deprecation of VPS4 snapshot with id: {}", snapshotIdToBeDeprecated);
            context.execute("ReverseSnapshotDeprecation" + snapshotIdToBeDeprecated, ctx -> {
                vps4SnapshotService.updateSnapshotStatus(snapshotIdToBeDeprecated, SnapshotStatus.LIVE);
                return null;
            }, Void.class);
        }
    }

    private void updateHfsSnapshotId(UUID vps4SnapshotId, long hfsSnapshotId) {
        logger.info("Update VPS4 snapshot [{}] with HFS snapshot id: {}", vps4SnapshotId, hfsSnapshotId);
        context.execute("UpdateHfsSnapshotId" + vps4SnapshotId, ctx -> {
            vps4SnapshotService.updateHfsSnapshotId(vps4SnapshotId, hfsSnapshotId);
            return null;
        }, Void.class);
    }

    private void updateHfsImageId(UUID vps4SnapshotId, long hfsSnapshotId) {
        gdg.hfs.vhfs.snapshot.Snapshot hfsSnapshot = context.execute(
                "GetHFSSnapshot",
                ctx -> hfsSnapshotService.getSnapshot(hfsSnapshotId),
                gdg.hfs.vhfs.snapshot.Snapshot.class
        );

        logger.info("Update VPS4 snapshot [{}] with Nocfox image id: {}", vps4SnapshotId, hfsSnapshot.imageId);

        context.execute("UpdateHfsImageId" + vps4SnapshotId, ctx -> {
            vps4SnapshotService.updateHfsImageId(vps4SnapshotId, hfsSnapshot.imageId);
            return null;
        }, Void.class);
    }

    private void deleteVmHvForSnapshotTracking (UUID vmId) {
        logger.info("Deleting hypervisor info that was used to track snapshot for VM {}.", vmId);
        context.execute("DeleteVmHvForSnapshotTracking" + vmId, ctx -> {
            vps4SnapshotService.deleteVmHvForSnapshotTracking(vmId);
            return null;
        }, Void.class);
    }

    public static class Request extends Vps4ActionRequest {
        public long hfsVmId;
        public UUID vps4SnapshotId;
        public UUID orionGuid;
        public SnapshotType snapshotType;
        public String shopperId;
        public String initiatedBy;
        public boolean allowRetries = true;
    }

    public static class Response {
        public SnapshotAction hfsAction;
    }

}
