package com.godaddy.vps4.orchestration.scheduler;

import com.godaddy.hfs.config.Config;
import com.godaddy.vps4.client.ClientCertAuth;
import com.godaddy.vps4.scheduler.api.core.JobType;
import com.godaddy.vps4.scheduler.api.core.SchedulerJobDetail;
import com.godaddy.vps4.scheduler.api.core.utils.Utils;
import com.godaddy.vps4.scheduler.api.plugin.Vps4BackupJobRequest;
import com.godaddy.vps4.scheduler.api.web.SchedulerWebService;
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

    private final SchedulerWebService schedulerWebService;
    private final Config config;

    @Inject
    public SetupAutomaticBackupSchedule(@ClientCertAuth SchedulerWebService schedulerWebService, Config config) {
        this.schedulerWebService = schedulerWebService;
        this.config = config;
    }

    @Override
    public UUID execute(CommandContext context, SetupAutomaticBackupSchedule.Request request) {
        String product = Utils.getProductForJobRequestClass(Vps4BackupJobRequest.class);
        String jobGroup = Utils.getJobGroupForJobRequestClass(Vps4BackupJobRequest.class);
        Vps4BackupJobRequest backupRequest = getJobRequestData(request);

        try {
            SchedulerJobDetail jobDetail = context.execute(
                    "Create schedule",
                    ctx -> schedulerWebService.submitJobToGroup(product, jobGroup, backupRequest),
                    SchedulerJobDetail.class);
            return jobDetail.id;
        } catch (Exception e) {
            logger.error("Error while creating a backup schedule for VM: {}. Error details: {}", request.vmId, e);
            throw new RuntimeException(e);
        }
    }

    private Vps4BackupJobRequest getJobRequestData(Request request) {
        Vps4BackupJobRequest backupRequest = new Vps4BackupJobRequest();
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
