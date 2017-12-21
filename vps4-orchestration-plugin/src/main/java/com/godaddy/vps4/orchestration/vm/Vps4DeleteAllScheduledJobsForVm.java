package com.godaddy.vps4.orchestration.vm;

import com.godaddy.vps4.orchestration.scheduler.DeleteScheduledJob;
import com.godaddy.vps4.orchestration.scheduler.Utils;
import com.godaddy.vps4.scheduledJob.ScheduledJob;
import com.godaddy.vps4.scheduledJob.ScheduledJobService;
import com.google.inject.Inject;
import gdg.hfs.orchestration.Command;
import gdg.hfs.orchestration.CommandContext;
import gdg.hfs.orchestration.CommandMetadata;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.UUID;

@CommandMetadata(
            name="Vps4DeleteAllScheduledJobsForVm",
            requestType=UUID.class,
            responseType=Void.class
)
public class Vps4DeleteAllScheduledJobsForVm implements Command<UUID, Void> {

    private static final Logger logger = LoggerFactory.getLogger(Vps4DeleteAllScheduledJobsForVm.class);

    private final ScheduledJobService scheduledJobService;

    private CommandContext context;
    private UUID vmId;

    @Inject
    public Vps4DeleteAllScheduledJobsForVm(ScheduledJobService scheduledJobService) {
        this.scheduledJobService = scheduledJobService;
    }

    
    @Override
    public Void execute(CommandContext context, UUID vmId) {
        this.context = context;
        this.vmId = vmId;
        deleteAllScheduledJobs();
        return null;
    }

    private void deleteAllScheduledJobs() {
        @SuppressWarnings("unchecked")
        List<ScheduledJob> jobs =  scheduledJobService.getScheduledJobs(vmId);
        
        for(ScheduledJob job : jobs) {
            deleteJob(job);
        }
    }

    private void deleteJob(ScheduledJob job) {
        DeleteScheduledJob.Request req = new DeleteScheduledJob.Request();
        req.jobId = job.id;
        req.jobRequestClass = Utils.getJobRequestClassForType(job.type);

        try {
            logger.info("Delete scheduled job type {} with id {}", job.type, job.id);
            context.execute(String.format("DeleteScheduledJob-%s", job.id), DeleteScheduledJob.class, req);
        }
        catch (RuntimeException e) {
            // Squelch this as this might be a valid exception as the job we are trying
            // to delete may no longer exist. this is because, right now we only track jobs when they are created
            // but we don't untrack them, ex: in case of a one_time jobs after it has fired.
            logger.info("They was an error while deleting job id: [{}] for vm: [{}]. {}", job.id, vmId, e);
        }
    }
}
