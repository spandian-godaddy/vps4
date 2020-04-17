package com.godaddy.vps4.orchestration.scheduler;

import java.time.Instant;
import java.util.UUID;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.godaddy.vps4.scheduler.api.core.JobType;
import com.godaddy.vps4.scheduler.api.core.SchedulerJobDetail;
import com.godaddy.vps4.scheduler.api.core.utils.Utils;
import com.godaddy.vps4.scheduler.api.plugin.Vps4ZombieCleanupJobRequest;
import com.godaddy.vps4.scheduler.api.web.SchedulerWebService;

import gdg.hfs.orchestration.Command;
import gdg.hfs.orchestration.CommandContext;
import gdg.hfs.orchestration.CommandMetadata;
import gdg.hfs.orchestration.CommandRetryStrategy;


@CommandMetadata(
        name = "RescheduleZombieVmCleanup",
        requestType = RescheduleZombieVmCleanup.Request.class,
        responseType = Void.class,
        retryStrategy = CommandRetryStrategy.NEVER
)
public class RescheduleZombieVmCleanup implements Command<RescheduleZombieVmCleanup.Request, Void> {

    private static final Logger logger = LoggerFactory.getLogger(RescheduleZombieVmCleanup.class);

    private final SchedulerWebService schedulerWebService;

    @Inject
    public RescheduleZombieVmCleanup(SchedulerWebService schedulerWebService) {
        this.schedulerWebService = schedulerWebService;
    }

    @Override
    public Void execute(CommandContext context, RescheduleZombieVmCleanup.Request request) {

        String product = Utils.getProductForJobRequestClass(Vps4ZombieCleanupJobRequest.class);
        String jobGroup = Utils.getJobGroupForJobRequestClass(Vps4ZombieCleanupJobRequest.class);
        Vps4ZombieCleanupJobRequest zombieCleanupJobRequest = getJobRequestData(request.vmId, request.when);

        try {
            SchedulerJobDetail jobDetail = context.execute(
                    "Reschedule Cleanup Zombie VM",
                    ctx -> schedulerWebService.rescheduleJob(product, jobGroup, request.jobId, zombieCleanupJobRequest),
                    SchedulerJobDetail.class);
        } catch (Exception e) {
            logger.error("Error while re-scheduling zombie cleanup job for VM: {}. Error details: {}", request.vmId, e);
            throw new RuntimeException(e);
        }
        return null;
    }

    private Vps4ZombieCleanupJobRequest getJobRequestData(UUID vmId, Instant when) {
        Vps4ZombieCleanupJobRequest zombieCleanupRequest = new Vps4ZombieCleanupJobRequest();
        zombieCleanupRequest.vmId = vmId;
        zombieCleanupRequest.jobType = JobType.ONE_TIME;
        zombieCleanupRequest.when = when;
        return zombieCleanupRequest;
    }

    public static class Request {
        public UUID vmId;
        public Instant when;
        public UUID jobId;
    }
}
