package com.godaddy.vps4.orchestration.scheduler;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.godaddy.vps4.scheduledJob.ScheduledJob;
import com.godaddy.vps4.scheduler.api.core.JobType;
import com.godaddy.vps4.scheduler.api.core.SchedulerJobDetail;
import com.godaddy.vps4.scheduler.api.core.utils.Utils;
import com.godaddy.vps4.scheduler.api.plugin.Vps4BackupJobRequest;
import com.godaddy.vps4.scheduler.api.web.SchedulerWebService;

import gdg.hfs.orchestration.Command;
import gdg.hfs.orchestration.CommandContext;

public class ScheduleAutomaticBackupRetry implements Command<ScheduleAutomaticBackupRetry.Request, UUID> {

    private static final Logger logger = LoggerFactory.getLogger(ScheduleAutomaticBackupRetry.class);

    private final SchedulerWebService schedulerService;

    @Inject
    public ScheduleAutomaticBackupRetry(SchedulerWebService schedulerService) {
        this.schedulerService = schedulerService;
    }

    @Override
    public UUID execute(CommandContext context, ScheduleAutomaticBackupRetry.Request request) {
        String product = Utils.getProductForJobRequestClass(Vps4BackupJobRequest.class);
        String jobGroup = Utils.getJobGroupForJobRequestClass(Vps4BackupJobRequest.class);
        Vps4BackupJobRequest backupRequest = getJobRequestData(request);

        try {
            SchedulerJobDetail jobDetail = context.execute(
                    "Retry Scheduled Backup",
                    ctx -> schedulerService.submitJobToGroup(product, jobGroup, backupRequest),
                    SchedulerJobDetail.class);
            return jobDetail.id;
        } catch (Exception e) {
            logger.error("Error while rescheduling errored backup for VM: {}. Error details: {}", request.vmId, e);
            throw new RuntimeException(e);
        }
    }

    private Vps4BackupJobRequest getJobRequestData(Request request) {
        Vps4BackupJobRequest backupRequest = new Vps4BackupJobRequest();
        backupRequest.vmId = request.vmId;
        backupRequest.backupName = "auto-backup";
        backupRequest.jobType = JobType.ONE_TIME;
        backupRequest.shopperId = request.shopperId;
        backupRequest.scheduledJobType = ScheduledJob.ScheduledJobType.BACKUPS_RETRY;
        backupRequest.when = Instant.now().plus(request.minutesToWait, ChronoUnit.MINUTES);
        return backupRequest;
    }

    public static class Request {
        public UUID vmId;
        public String shopperId;
        public int minutesToWait;
    }
}
