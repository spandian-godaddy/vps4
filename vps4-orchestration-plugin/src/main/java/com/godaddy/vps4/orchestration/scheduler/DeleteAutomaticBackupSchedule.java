package com.godaddy.vps4.orchestration.scheduler;

import com.godaddy.vps4.client.ClientCertAuth;
import com.godaddy.vps4.scheduler.core.utils.Utils;
import com.godaddy.vps4.scheduler.plugin.backups.Vps4BackupJob;
import com.godaddy.vps4.scheduler.web.client.SchedulerService;
import gdg.hfs.orchestration.Command;
import gdg.hfs.orchestration.CommandContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.util.UUID;

public class DeleteAutomaticBackupSchedule implements Command<UUID, Void> {

    private static final Logger logger = LoggerFactory.getLogger(DeleteAutomaticBackupSchedule.class);

    private final SchedulerService schedulerService;

    @Inject
    public DeleteAutomaticBackupSchedule(@ClientCertAuth SchedulerService schedulerService) {
        this.schedulerService = schedulerService;
    }

    @Override
    public Void execute(CommandContext context, UUID backupJobId) {
        String product = Utils.getProductForJobClass(Vps4BackupJob.class);
        String jobGroup = Utils.getJobGroupForJobClass(Vps4BackupJob.class);

        try {
            context.execute(
                "Delete schedule",
                ctx -> {
                    schedulerService.deleteJob(product, jobGroup, backupJobId);
                    return null;
                },
                Void.class);
            return null;
        } catch (Exception e) {
            logger.error("Error while deleting backup schedule with id: {}. Error details: {}", backupJobId, e);
            throw new RuntimeException(e);
        }
    }
}
