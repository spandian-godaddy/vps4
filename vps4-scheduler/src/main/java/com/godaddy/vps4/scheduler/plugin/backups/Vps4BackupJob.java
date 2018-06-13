package com.godaddy.vps4.scheduler.plugin.backups;

import com.godaddy.vps4.scheduler.api.plugin.Vps4BackupJobRequest;
import com.godaddy.vps4.scheduler.core.JobMetadata;
import com.godaddy.vps4.scheduler.core.SchedulerJob;
import com.godaddy.vps4.snapshot.SnapshotType;
import com.godaddy.vps4.web.client.VmSnapshotService;
import com.godaddy.vps4.snapshot.SnapshotAction;
import com.godaddy.vps4.web.vm.VmSnapshotResource;
import static com.godaddy.vps4.client.ClientUtils.withShopperId;
import com.google.inject.Inject;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.UUID;

@JobMetadata(
    product = "vps4",
    jobGroup = "backups",
    jobRequestType = Vps4BackupJobRequest.class
)
public class Vps4BackupJob extends SchedulerJob {
    private static final Logger logger = LoggerFactory.getLogger(Vps4BackupJob.class);

    @Inject VmSnapshotService vmSnapshotService;

    Vps4BackupJobRequest request;

    @Override
    public void execute(JobExecutionContext context) throws JobExecutionException {
        try {
            createAutomaticBackup(request.vmId, request.backupName, request.shopperId);
        }
        catch (Exception e) {
            logger.error("Error while processing backup job for vm {}. {}", request.vmId, e);
            // don't set flag to reschedule immediately
            // Rescheduling a failed backup creation should be handled in a JobListener (Quartz)
            throw new JobExecutionException(e);
        }
    }

    private void createAutomaticBackup(UUID vmId, String backupName, String shopperId) {
        VmSnapshotResource.VmSnapshotRequest vmSnapshotRequest = new VmSnapshotResource.VmSnapshotRequest();
        vmSnapshotRequest.name = backupName;
        vmSnapshotRequest.snapshotType = SnapshotType.AUTOMATIC;
        logger.info("Creating backup for vm {}", vmId);
        SnapshotAction action = withShopperId(shopperId,
                () -> vmSnapshotService.createSnapshot(vmId, vmSnapshotRequest),
                SnapshotAction.class);
        logger.info("Automatic backup {} created for vm {}, ", action.snapshotId, vmId);
    }

    public void setRequest(Vps4BackupJobRequest request) {
        this.request = request;
    }
}
