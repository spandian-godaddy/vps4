package com.godaddy.vps4.orchestration.ohbackup;

import java.util.UUID;
import java.util.function.Function;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.godaddy.vps4.oh.OhBackupDataService;
import com.godaddy.vps4.oh.backups.OhBackupData;
import com.godaddy.vps4.oh.backups.OhBackupService;
import com.godaddy.vps4.oh.backups.models.OhBackup;
import com.godaddy.vps4.orchestration.ActionCommand;
import com.godaddy.vps4.orchestration.snapshot.Vps4DeprecateSnapshot;
import com.godaddy.vps4.orchestration.vm.VmActionRequest;
import com.godaddy.vps4.snapshot.Snapshot;
import com.godaddy.vps4.snapshot.SnapshotService;
import com.godaddy.vps4.snapshot.SnapshotStatus;
import com.godaddy.vps4.snapshot.SnapshotType;
import com.godaddy.vps4.vm.ActionService;
import com.godaddy.vps4.vm.VirtualMachine;
import com.google.inject.Inject;

import gdg.hfs.orchestration.CommandContext;
import gdg.hfs.orchestration.CommandMetadata;
import gdg.hfs.orchestration.CommandRetryStrategy;

@CommandMetadata(
        name = "Vps4CreateOhBackup",
        requestType = Vps4CreateOhBackup.Request.class,
        retryStrategy = CommandRetryStrategy.NEVER
)
public class Vps4CreateOhBackup extends ActionCommand<Vps4CreateOhBackup.Request, Void> {
    private static final long OPEN_SLOTS_PER_CREDIT = 1;

    private static final Logger logger = LoggerFactory.getLogger(Vps4CreateOhBackup.class);
    private final OhBackupService ohBackupService;
    private final OhBackupDataService ohBackupDataService;
    private final SnapshotService snapshotService;

    private CommandContext context;
    private String initiatedBy;
    private String name;
    private VirtualMachine vm;

    private boolean isAtQuotaLimit;
    private OhBackupData oldestBackupData;
    private Snapshot oldestSnapshot;

    @Inject
    public Vps4CreateOhBackup(ActionService actionService,
                              OhBackupService ohBackupService,
                              OhBackupDataService ohBackupDataService,
                              SnapshotService snapshotService) {
        super(actionService);
        this.ohBackupService = ohBackupService;
        this.ohBackupDataService = ohBackupDataService;
        this.snapshotService = snapshotService;
    }

    public static class Request extends VmActionRequest {
        public String initiatedBy;
        public String name;

        public Request() {} // needed for deserialization

        public Request(VirtualMachine virtualMachine, String initiatedBy, String name) {
            this.virtualMachine = virtualMachine;
            this.initiatedBy = initiatedBy;
            this.name = name;
        }
    }

    @Override
    protected Void executeWithAction(CommandContext context, Request request) {
        this.context = context;
        this.initiatedBy = request.initiatedBy;
        this.name = request.name;
        this.vm = request.virtualMachine;

        this.isAtQuotaLimit = isAtQuotaLimit();
        this.oldestBackupData = getOldestBackupData();
        this.oldestSnapshot = getOldestSnapshot();

        cancelErroredSnapshots();

        if (shouldDestroyOldestSnapshot()) {
            markOldestSnapshotForDeprecation();
        }

        createOhBackup();

        if (shouldDestroyOldestOhBackup()) {
            context.execute(Vps4DestroyOhBackup.class,
                            new Vps4DestroyOhBackup.Request(vm, oldestBackupData.backupId));
        }
        if (shouldDestroyOldestSnapshot()) {
            context.execute(Vps4DeprecateSnapshot.class,
                            new Vps4DeprecateSnapshot.Request(vm.vmId, oldestSnapshot.id, initiatedBy));
        }
        return null;
    }

    private boolean isAtQuotaLimit() {
        long count = ohBackupDataService.totalFilledSlots(vm.vmId)
                + snapshotService.totalFilledSlots(vm.orionGuid, SnapshotType.ON_DEMAND);
        return count >= OPEN_SLOTS_PER_CREDIT;
    }

    private OhBackupData getOldestBackupData() {
        return ohBackupDataService.getOldestBackup(vm.vmId);
    }

    private Snapshot getOldestSnapshot() {
        return snapshotService.getOldestLiveSnapshot(vm.orionGuid, SnapshotType.ON_DEMAND);
    }

    private void cancelErroredSnapshots() {
        context.execute("CancelErroredSnapshots", ctx -> {
            snapshotService.cancelErroredSnapshots(vm.orionGuid, SnapshotType.ON_DEMAND);
            return null;
        }, Void.class);
    }

    private void markOldestSnapshotForDeprecation() {
        context.execute("MarkOldestSnapshotForDeprecation-" + vm.orionGuid,
                        ctx -> snapshotService.markOldestSnapshotForDeprecation(vm.orionGuid, SnapshotType.ON_DEMAND),
                        UUID.class);
    }

    private boolean shouldDestroyOldestOhBackup() {
        if (!isAtQuotaLimit || oldestBackupData == null) {
            return false;
        }
        if (oldestSnapshot == null) {
            return true;
        }
        return oldestBackupData.created.isBefore(oldestSnapshot.createdAt);
    }

    private boolean shouldDestroyOldestSnapshot() {
        if (!isAtQuotaLimit || oldestSnapshot == null) {
            return false;
        }
        if (oldestBackupData == null) {
            return true;
        }
        return oldestSnapshot.createdAt.isBefore(oldestBackupData.created);
    }

    private void createOhBackup() {
        try {
            Function<CommandContext, OhBackup> fn = ctx -> {
                OhBackup backup = ohBackupService.createBackup(vm.vmId);
                ohBackupDataService.createBackup(backup.id, vm.vmId, name);
                return backup;
            };
            OhBackup backup = context.execute("CreateOhBackup", fn, OhBackup.class);
            WaitForOhJob.Request waitRequest = new WaitForOhJob.Request();
            waitRequest.vmId = vm.vmId;
            waitRequest.jobId = backup.jobId;
            context.execute(WaitForOhJob.class, waitRequest);
        } catch (Exception e) {
            reverseSnapshotDeprecation();
            throw new RuntimeException("Exception while creating OH backup for VM ID " + vm.vmId, e);
        }
    }

    private void reverseSnapshotDeprecation() {
        if (shouldDestroyOldestSnapshot()) {
            logger.info("Reverse deprecation of VPS4 snapshot with id: {}", oldestSnapshot.id);
            context.execute("ReverseSnapshotDeprecation" + oldestSnapshot.id, ctx -> {
                snapshotService.updateSnapshotStatus(oldestSnapshot.id, SnapshotStatus.LIVE);
                return null;
            }, Void.class);
        }
    }
}
