package com.godaddy.vps4.orchestration.vm;

import java.util.List;
import java.util.UUID;

import gdg.hfs.orchestration.CommandRetryStrategy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.godaddy.vps4.orchestration.scheduler.DeleteScheduledJob;
import com.godaddy.vps4.orchestration.scheduler.Utils;
import com.godaddy.vps4.scheduledJob.ScheduledJob;
import com.godaddy.vps4.scheduledJob.ScheduledJobService;
import com.google.inject.Inject;

import gdg.hfs.orchestration.Command;
import gdg.hfs.orchestration.CommandContext;
import gdg.hfs.orchestration.CommandMetadata;

@CommandMetadata(
            name="Vps4DeleteAllScheduledJobsForVm",
            requestType=UUID.class,
            retryStrategy = CommandRetryStrategy.NEVER
)
public class Vps4DeleteAllScheduledJobsForVm implements Command<UUID, Void> {

    private static final Logger logger = LoggerFactory.getLogger(Vps4DeleteAllScheduledJobsForVm.class);

    private final ScheduledJobService scheduledJobService;

    private CommandContext context;
    protected UUID vmId;

    @Inject
    public Vps4DeleteAllScheduledJobsForVm(ScheduledJobService scheduledJobService) {
        this.scheduledJobService = scheduledJobService;
    }

    
    @Override
    public Void execute(CommandContext context, UUID vmId) {
        this.context = context;
        this.vmId = vmId;
        List<ScheduledJob> jobs = getJobsForDeletion();
        for (ScheduledJob job : jobs) {
            deleteJob(job);
        }
        return null;
    }

    protected List<ScheduledJob> getJobsForDeletion() {
        return scheduledJobService.getScheduledJobs(vmId);
    }

    private void deleteJob(ScheduledJob job) {
        DeleteScheduledJob.Request req = new DeleteScheduledJob.Request();
        req.jobId = job.id;
        req.jobRequestClass = Utils.getJobRequestClassForType(job.type);
        scheduledJobService.deleteScheduledJob(job.id);

        try {
            logger.info("Delete scheduled job type {} with id {}", job.type, job.id);
            context.execute(String.format("DeleteScheduledJob-%s", job.id), DeleteScheduledJob.class, req);
        }
        catch (RuntimeException e) {
            // Squelch this as this might be a valid exception as the job we are trying
            // to delete may no longer exist. this is because, right now we only track jobs when they are created
            // but we don't untrack them, ex: in case of a one_time jobs after it has fired.
            logger.info("There was an error while deleting job id: [{}] for vm: [{}]. This may be expected behavior if the job no longer exists. {}", job.id, vmId, e);
        }
    }
}
