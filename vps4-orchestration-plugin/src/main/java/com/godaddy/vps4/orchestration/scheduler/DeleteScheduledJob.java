package com.godaddy.vps4.orchestration.scheduler;

import com.godaddy.vps4.scheduler.api.core.JobRequest;
import com.godaddy.vps4.scheduler.api.core.utils.Utils;
import com.godaddy.vps4.scheduler.api.plugin.Vps4ZombieCleanupJobRequest;
import com.godaddy.vps4.scheduler.api.web.SchedulerWebService;
import gdg.hfs.orchestration.Command;
import gdg.hfs.orchestration.CommandContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.util.UUID;

public class DeleteScheduledJob implements Command<DeleteScheduledJob.Request, Void> {

    private static final Logger logger = LoggerFactory.getLogger(DeleteScheduledJob.class);

    private final SchedulerWebService schedulerWebService;

    @Inject
    public DeleteScheduledJob(SchedulerWebService schedulerWebService) {
        this.schedulerWebService = schedulerWebService;
    }

    @Override
    public Void execute(CommandContext context, DeleteScheduledJob.Request request) {
        String product = Utils.getProductForJobRequestClass(request.jobRequestClass);
        String jobGroup = Utils.getJobGroupForJobRequestClass(request.jobRequestClass);

        try {
            context.execute(
                String.format("Delete job with id: %s", request.jobId),
                ctx -> {
                    schedulerWebService.deleteJob(product, jobGroup, request.jobId);
                    return null;
                },
                Void.class);
            return null;
        } catch (Exception e) {
            logger.error("Error while deleting job schedule with id: {}. Error details: {}", request.jobId, e);
            throw new RuntimeException(e);
        }
    }
    
    public static class Request {
        public UUID jobId;
        public Class<? extends JobRequest> jobRequestClass;
    }
}
