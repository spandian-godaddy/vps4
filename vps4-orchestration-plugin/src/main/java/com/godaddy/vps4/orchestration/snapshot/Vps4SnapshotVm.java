package com.godaddy.vps4.orchestration.snapshot;

import java.util.Arrays;
import java.util.UUID;

import javax.inject.Inject;

import org.apache.commons.lang3.RandomStringUtils;
import org.json.simple.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.godaddy.hfs.config.Config;
import com.godaddy.hfs.vm.Vm;
import com.godaddy.hfs.vm.VmService;
import com.godaddy.vps4.orchestration.ActionCommand;
import com.godaddy.vps4.orchestration.Vps4ActionRequest;
import com.godaddy.vps4.orchestration.scheduler.ScheduleAutomaticBackupRetry;
import com.godaddy.vps4.orchestration.vm.Vps4RecordScheduledJobForVm;
import com.godaddy.vps4.scheduledJob.ScheduledJob;
import com.godaddy.vps4.snapshot.Snapshot;
import com.godaddy.vps4.snapshot.SnapshotActionService;
import com.godaddy.vps4.snapshot.SnapshotService;
import com.godaddy.vps4.snapshot.SnapshotType;
import com.godaddy.vps4.util.TroubleshootVmService;
import com.godaddy.vps4.vm.ActionService;
import com.godaddy.vps4.vm.ActionType;
import com.godaddy.vps4.vm.VirtualMachine;
import com.godaddy.vps4.vm.VirtualMachineService;

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

    private final TroubleshootVmService troubleshootVmService;
    private final gdg.hfs.vhfs.snapshot.SnapshotService hfsSnapshotService;
    private final SnapshotService vps4SnapshotService;
    private final VirtualMachineService virtualMachineService;
    private final VmService vmService;
    private UUID snapshotIdToBeDeprecated;
    private final Config config;

    @Inject
    public Vps4SnapshotVm(@SnapshotActionService ActionService actionService,
                          TroubleshootVmService troubleshootVmService,
                          gdg.hfs.vhfs.snapshot.SnapshotService hfsSnapshotService,
                          SnapshotService vps4SnapshotService,
                          VirtualMachineService virtualMachineService,
                          VmService vmService,
                          Config config) {
        super(actionService);
        this.troubleshootVmService = troubleshootVmService;
        this.hfsSnapshotService = hfsSnapshotService;
        this.vps4SnapshotService = vps4SnapshotService;
        this.virtualMachineService = virtualMachineService;
        this.vmService = vmService;
        this.config = config;
    }

    @Override
    protected Response executeWithAction(CommandContext context, Request request) {
        this.context = context;
        throwErrorIfAgentIsDown(request);
        throwErrorAndRescheduleIfLimitReached(request);
        snapshotIdToBeDeprecated = context.execute("MarkOldestSnapshotForDeprecation" + request.orionGuid,
                ctx -> vps4SnapshotService.markOldestSnapshotForDeprecation(request.orionGuid, request.snapshotType),
                UUID.class);
        context.execute("CancelErroredSnapshots", ctx -> {
            vps4SnapshotService.cancelErroredSnapshots(request.orionGuid, request.snapshotType);
            return null;
        }, Void.class);
        SnapshotAction hfsAction = createAndWaitForSnapshotCompletion(request);
        deprecateOldSnapshot(request.initiatedBy);
        return generateResponse(hfsAction);
    }

    private void throwErrorIfAgentIsDown(Request request) {
        VirtualMachine vm = virtualMachineService.getVirtualMachine(request.vmId);
        Vm hfsVm = vmService.getVm(request.hfsVmId);
        if (hfsVm.status.equals("ACTIVE")) {
            if (!troubleshootVmService.isPortOpenOnVm(vm.primaryIpAddress.ipAddress, 2224)) {
                throw new RuntimeException("VmId " + request.vmId + " has port 2224 blocked. Refusing to take snapshot.");
            }
            if (!troubleshootVmService.getHfsAgentStatus(vm.hfsVmId).equals("OK")) {
                throw new RuntimeException("Agent for vmId " + request.vmId + " is down. Refusing to take snapshot.");
            }
        }
    }

    private void throwErrorAndRescheduleIfLimitReached(Request request) {
        if (request.snapshotType.equals(SnapshotType.AUTOMATIC)) {
            int currentLoad = vps4SnapshotService.totalSnapshotsInProgress();
            int maxAllowed = Integer.parseInt(config.get("vps4.autobackup.concurrentLimit", "30"));
            if (currentLoad >= maxAllowed) {
                handleTooManyConcurrentSnapshots(request);
            }
        }
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
                vps4SnapshotService.markSnapshotAsDeprecated(snapshotIdToBeDeprecated);
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
            logger.info("Snapshot creation error (waitForAction) for snapshot with id: {}", request.vps4SnapshotId);
            vps4SnapshotService.markSnapshotErrored(request.vps4SnapshotId);
            reverseSnapshotDeprecation();
            handleSnapshotCreationError(request, e);
        }

        context.execute("MarkSnapshotLive" + request.vps4SnapshotId, ctx -> {
            vps4SnapshotService.markSnapshotLive(request.vps4SnapshotId);
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
        if (request.snapshotType.equals(SnapshotType.AUTOMATIC)) {
            if (shouldRetryAgain(request.vmId)) {
                int minutesToWait = Integer.parseInt(config.get("vps4.autobackup.rescheduleFailedBackupWaitMinutes", "720"));
                rescheduleSnapshot(request, minutesToWait);
                vps4SnapshotService.markSnapshotErrorRescheduled(request.vps4SnapshotId);
            } else {
                logger.warn("Max retries exceeded for automatic snapshot on vm: {}  Will not retry again.",
                            request.vmId);
            }
        }
        throw new RuntimeException("Exception while running backup for vmId " + request.vmId, e);
    }

    private void handleTooManyConcurrentSnapshots(Request request) {
        int minutesToWait = Integer.parseInt(config.get("vps4.autobackup.rescheduleConcurrentBackupWaitMinutes", "20"));
        int delta = Integer.parseInt(config.get("vps4.autobackup.rescheduleConcurrentBackupWaitDelta", "10"));
        minutesToWait += Math.random() * (2 * delta) - delta;
        rescheduleSnapshot(request, minutesToWait);
        vps4SnapshotService.markSnapshotLimitRescheduled(request.vps4SnapshotId);
        throw new RuntimeException("Too many concurrent snapshots. Rescheduling for vmId " + request.vmId);
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
        String name = String.format("vps4-%s-%s",
                request.snapshotType.name().toLowerCase().substring(0, 4),
                RandomStringUtils.randomAlphabetic(8));

        try {
            SnapshotAction hfsAction = context.execute(
                    "Vps4SnapshotVm",
                    ctx -> hfsSnapshotService.createSnapshot(vmId, name, version, true, false),
                    SnapshotAction.class
            );

            logger.info("HFS snapshot create request returned action: {}", hfsAction);
            context.execute("MarkSnapshotInProgress" + request.vps4SnapshotId, ctx -> {
                vps4SnapshotService.markSnapshotInProgress(request.vps4SnapshotId);
                return null;
            }, Void.class);
            return hfsAction;
        } catch (Exception e) {
            logger.info("Snapshot creation error for VPS4 snapshot with id: {}", request.vps4SnapshotId);
            vps4SnapshotService.markSnapshotErrored(request.vps4SnapshotId);
            reverseSnapshotDeprecation();
            handleSnapshotCreationError(request, e);
            return null;
        }
    }

    private void reverseSnapshotDeprecation() {
        if (snapshotIdToBeDeprecated != null)
        {
            logger.info("Reverse deprecation of VPS4 snapshot with id: {}", snapshotIdToBeDeprecated);
            context.execute("ReverseSnapshotDeprecation" + snapshotIdToBeDeprecated, ctx -> {
            vps4SnapshotService.reverseSnapshotDeprecation(snapshotIdToBeDeprecated);
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

    public static class Request extends Vps4ActionRequest {
        public long hfsVmId;
        public UUID vps4SnapshotId;
        public UUID orionGuid;
        public SnapshotType snapshotType;
        public String shopperId;
        public String initiatedBy;
    }

    public static class Response {
        public SnapshotAction hfsAction;
    }

}
