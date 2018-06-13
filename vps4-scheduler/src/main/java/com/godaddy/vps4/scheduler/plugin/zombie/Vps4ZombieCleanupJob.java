package com.godaddy.vps4.scheduler.plugin.zombie;

import com.godaddy.vps4.scheduler.api.plugin.Vps4ZombieCleanupJobRequest;
import com.godaddy.vps4.scheduler.core.JobMetadata;
import com.godaddy.vps4.scheduler.core.SchedulerJob;
import com.godaddy.vps4.web.client.VmService;
import com.godaddy.vps4.vm.VmAction;
import com.google.inject.Inject;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.UUID;

@JobMetadata(
    product = "vps4",
    jobGroup = "zombie",
    jobRequestType = Vps4ZombieCleanupJobRequest.class
)
public class Vps4ZombieCleanupJob extends SchedulerJob {
    private static final Logger logger = LoggerFactory.getLogger(Vps4ZombieCleanupJob.class);

    @Inject VmService vmService;

    Vps4ZombieCleanupJobRequest request;

    @Override
    public void execute(JobExecutionContext context) throws JobExecutionException {
        try {
            cleanupZombieVm(request.vmId);
        }
        catch (Exception e) {
            logger.error("Error while processing zombie cleanup job for vm {}. {}", request.vmId, e);
            // don't set flag to reschedule immediately
            // Rescheduling a failed backup creation should be handled in a JobListener (Quartz)
            throw new JobExecutionException(e);
        }
    }

    private void cleanupZombieVm(UUID vmId) {
        logger.info("Cleaning up zombie vm {}", vmId);
        VmAction action = vmService.destroyVm(vmId);
        logger.info("Zombie vm {} deletion request submitted. Action {}, ", vmId, action.id);
    }

    public void setRequest(Vps4ZombieCleanupJobRequest request) {
        this.request = request;
    }
}
