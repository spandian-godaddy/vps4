package com.godaddy.vps4.orchestration.scheduler;

import com.godaddy.vps4.scheduler.api.core.utils.Utils;
import com.godaddy.vps4.scheduler.api.plugin.Vps4BackupJobRequest;
import com.godaddy.vps4.scheduler.api.web.SchedulerWebService;
import gdg.hfs.orchestration.Command;
import gdg.hfs.orchestration.CommandContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.util.UUID;

public class DeleteAutomaticBackupSchedule implements Command<UUID, Void> {

    private static final Logger logger = LoggerFactory.getLogger(DeleteAutomaticBackupSchedule.class);

    private final SchedulerWebService schedulerWebService;

    @Inject
    public DeleteAutomaticBackupSchedule(SchedulerWebService schedulerWebService) {
        this.schedulerWebService = schedulerWebService;
    }

    @Override
    public Void execute(CommandContext context, UUID backupJobId) {
        try {
            String product = Utils.getProductForJobRequestClass(Vps4BackupJobRequest.class);
            String jobGroup = Utils.getJobGroupForJobRequestClass(Vps4BackupJobRequest.class);
            
            context.execute(
                "Delete schedule",
                ctx -> {
                    schedulerWebService.deleteJob(product, jobGroup, backupJobId);
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
