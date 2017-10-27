package com.godaddy.vps4.orchestration.scheduler;

import com.godaddy.hfs.config.Config;
import com.godaddy.vps4.client.ClientCertAuth;
import com.godaddy.vps4.scheduler.core.JobType;
import com.godaddy.vps4.scheduler.core.SchedulerJobDetail;
import com.godaddy.vps4.scheduler.core.utils.Utils;
import com.godaddy.vps4.scheduler.plugin.backups.Vps4BackupJob;
import com.godaddy.vps4.scheduler.web.client.SchedulerService;
import gdg.hfs.orchestration.Command;
import gdg.hfs.orchestration.CommandContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

public class SetupAutomaticBackupSchedule implements Command<SetupAutomaticBackupSchedule.Request, UUID> {

    private static final Logger logger = LoggerFactory.getLogger(SetupAutomaticBackupSchedule.class);

    private final SchedulerService schedulerService;
    private final Config config;

    @Inject
    public SetupAutomaticBackupSchedule(@ClientCertAuth SchedulerService schedulerService, Config config) {
        this.schedulerService = schedulerService;
        this.config = config;
    }

    @Override
    public UUID execute(CommandContext context, SetupAutomaticBackupSchedule.Request request) {
        String product = Utils.getProductForJobClass(Vps4BackupJob.class);
        String jobGroup = Utils.getJobGroupForJobClass(Vps4BackupJob.class);
        Vps4BackupJob.Request backupRequest = getJobRequestData(request);

        try {
            SchedulerJobDetail jobDetail = context.execute(
                    "Create schedule",
                    ctx -> schedulerService.submitJobToGroup(product, jobGroup, backupRequest),
                    SchedulerJobDetail.class);
            return jobDetail.id;
        } catch (Exception e) {
            logger.error("Error while creating a backup schedule for VM: {}. Error details: {}", request.vmId, e);
            throw new RuntimeException(e);
        }
    }

    private Vps4BackupJob.Request getJobRequestData(Request request) {
        Vps4BackupJob.Request backupRequest = new Vps4BackupJob.Request();
        backupRequest.vmId = request.vmId;
        backupRequest.backupName = request.backupName;
        backupRequest.jobType = JobType.RECURRING;
        long firstTime = Long.parseLong(config.get("vps4.autobackup.initial"));
        backupRequest.when = Instant.now().plus(firstTime, ChronoUnit.DAYS);
        int repeatInterval = Integer.parseInt(config.get("vps4.autobackup.repeatInterval"));
        backupRequest.repeatIntervalInDays = repeatInterval; // every seven days
        return backupRequest;
    }

    public static class Request {
        public UUID vmId;
        public String backupName;
    }
}
