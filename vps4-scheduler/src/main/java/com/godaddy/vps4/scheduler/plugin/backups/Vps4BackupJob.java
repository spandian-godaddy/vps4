package com.godaddy.vps4.scheduler.plugin.backups;

import com.godaddy.hfs.config.Config;
import com.godaddy.hfs.vm.VmAction;
import com.godaddy.vps4.scheduledJob.ScheduledJob;
import com.godaddy.vps4.scheduler.api.core.utils.Utils;
import com.godaddy.vps4.scheduler.api.plugin.Vps4BackupJobRequest;
import com.godaddy.vps4.scheduler.core.JobMetadata;
import com.godaddy.vps4.scheduler.core.SchedulerJob;
import com.godaddy.vps4.snapshot.Snapshot;
import com.godaddy.vps4.snapshot.SnapshotType;
import com.godaddy.vps4.vm.ServerType;
import com.godaddy.vps4.vm.VirtualMachine;
import com.godaddy.vps4.web.client.VmOhBackupService;
import com.godaddy.vps4.web.client.VmService;
import com.godaddy.vps4.web.client.VmSnapshotService;
import com.godaddy.vps4.snapshot.SnapshotAction;
import com.godaddy.vps4.web.ohbackup.OhBackupResource;
import com.godaddy.vps4.web.vm.VmSnapshotResource;
import static com.godaddy.vps4.client.ClientUtils.withShopperId;
import static org.quartz.TriggerBuilder.newTrigger;

import com.google.inject.Inject;

import org.json.simple.JSONObject;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.quartz.SchedulerException;
import org.quartz.Trigger;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Date;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

import javax.ws.rs.ClientErrorException;
import javax.ws.rs.core.Response;

@JobMetadata(
    product = "vps4",
    jobGroup = "backups",
    jobRequestType = Vps4BackupJobRequest.class
)
public class Vps4BackupJob extends SchedulerJob {
    private static final Logger logger = LoggerFactory.getLogger(Vps4BackupJob.class);

    @Inject private VmSnapshotService vmSnapshotService;
    @Inject private VmOhBackupService vmOhBackupService;
    @Inject private VmService vmService;
    @Inject private Config config;

    Vps4BackupJobRequest request;

    @Override
    public void execute(JobExecutionContext context) throws JobExecutionException {
        try {
            createAutomaticBackup(request.vmId, request.backupName, request.shopperId, request.scheduledJobType);
        } catch (ClientErrorException e) {
            handleClientErrorException(context, e);
        } catch (Exception e) {
            logger.error("Error while processing backup job for vm {}. {}", request.vmId, e);
            // don't set flag to reschedule immediately
            // Rescheduling a failed backup creation should be handled in a JobListener (Quartz)
            throw new JobExecutionException(e);
        }
    }

    private void handleClientErrorException(JobExecutionContext context, ClientErrorException e)  throws JobExecutionException{
        try {
            JSONObject entityJson = e.getResponse().readEntity(JSONObject.class);
            if ((e.getResponse().getStatus() == Response.Status.CONFLICT.getStatusCode()) &&
                    (entityJson.get("id").equals("SNAPSHOT_DC_LIMIT_REACHED") ||
                     entityJson.get("id").equals("SNAPSHOT_HV_LIMIT_REACHED"))) {
                int minutesToWait = Integer.parseInt(config.get("vps4.autobackup.rescheduleConcurrentBackupWaitMinutes", "20"));
                int delta = Integer.parseInt(config.get("vps4.autobackup.rescheduleConcurrentBackupWaitDelta", "10"));
                minutesToWait += Math.random() * (2 * delta) - delta;
                delayQuartzBackupJob(context, minutesToWait);
            }
            else {
                logger.error("Received ClientErrorException from vps4 api to create backup for vm {}. {}:{}",
                             request.vmId, e.getMessage(), entityJson.toJSONString());
                throw new JobExecutionException(e);
            }
        } catch (Exception ex) {
            logger.error("Error while processing backup job for vm {}. {}", request.vmId, ex);
            throw new JobExecutionException(ex);
        }
    }

    private void delayQuartzBackupJob(JobExecutionContext context, int minutesToWait) throws SchedulerException {

        String product = Utils.getProductForJobRequestClass(Vps4BackupJobRequest.class);
        String jobGroup = Utils.getJobGroupForJobRequestClass(Vps4BackupJobRequest.class);
        String jobGroupId = com.godaddy.vps4.scheduler.core.utils.Utils.getJobGroupId(product, jobGroup);

        Trigger newTrigger = newTrigger().withIdentity(UUID.randomUUID().toString(), jobGroupId)
                                         .startAt(Date.from(Instant.now().plus(minutesToWait, ChronoUnit.MINUTES)))
                                         .forJob(context.getJobDetail().getKey())
                                         .build();
        context.getScheduler().scheduleJob(newTrigger);
        logger.info("Delaying quartz scheduler job. One-time backup job for vm {} will run next at {}. Job detail: {}",
                    request.vmId, newTrigger.getNextFireTime(), context.getJobDetail().toString());
    }

    private void createAutomaticBackup(UUID vmId, String backupName, String shopperId, ScheduledJob.ScheduledJobType scheduledJobType) {
        logger.info("Creating backup for vm {}", vmId);
        VirtualMachine vm = vmService.getVm(vmId);
        if(vm.spec.serverType.platform == ServerType.Platform.OPTIMIZED_HOSTING){
            createOhBackup(vmId, backupName);
        }
        else {
            createHfsBackup(vmId, backupName, shopperId, scheduledJobType);
        }
    }

    private void createOhBackup(UUID vmId, String backupName) {
        OhBackupResource.OhBackupRequest request = new OhBackupResource.OhBackupRequest(backupName);
        vmOhBackupService.createOhBackup(vmId, request);
        logger.info("Scheduled backup created for vm {}, ", vmId);
    }

    private void createHfsBackup(UUID vmId, String backupName, String shopperId, ScheduledJob.ScheduledJobType scheduledJobType) {
        VmSnapshotResource.VmSnapshotRequest vmSnapshotRequest = new VmSnapshotResource.VmSnapshotRequest();
        vmSnapshotRequest.name = backupName;
        vmSnapshotRequest.snapshotType = getSnapshotType(scheduledJobType);
        SnapshotAction action = withShopperId(shopperId,
                () -> vmSnapshotService.createSnapshot(vmId, vmSnapshotRequest),
                SnapshotAction.class);
        logger.info("Scheduled backup {} created for vm {}, ", action.snapshotId, vmId);
    }

    private SnapshotType getSnapshotType(ScheduledJob.ScheduledJobType scheduledJobType){
        return (scheduledJobType == ScheduledJob.ScheduledJobType.BACKUPS_AUTOMATIC)
                ? SnapshotType.AUTOMATIC
                : SnapshotType.ON_DEMAND;
    }

    public void setRequest(Vps4BackupJobRequest request) {
        this.request = request;
    }
}
