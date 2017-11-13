package com.godaddy.vps4.orchestration.scheduler;

import com.godaddy.hfs.config.Config;
import com.godaddy.vps4.scheduler.api.core.JobType;
import com.godaddy.vps4.scheduler.api.core.SchedulerJobDetail;
import com.godaddy.vps4.scheduler.api.core.utils.Utils;
import com.godaddy.vps4.scheduler.api.plugin.Vps4ZombieCleanupJobRequest;
import com.godaddy.vps4.scheduler.api.web.SchedulerWebService;
import gdg.hfs.orchestration.Command;
import gdg.hfs.orchestration.CommandContext;
import gdg.hfs.orchestration.CommandMetadata;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.time.Instant;
import java.util.UUID;


@CommandMetadata(
        name="ScheduleZombieVmCleanup",
        requestType=ScheduleZombieVmCleanup.Request.class,
        responseType=UUID.class
)
public class ScheduleZombieVmCleanup implements Command<ScheduleZombieVmCleanup.Request, UUID> {

    private static final Logger logger = LoggerFactory.getLogger(ScheduleZombieVmCleanup.class);

    private final SchedulerWebService schedulerService;

    @Inject
    public ScheduleZombieVmCleanup(SchedulerWebService schedulerService) {
        this.schedulerService = schedulerService;
    }

    @Override
    public UUID execute(CommandContext context, ScheduleZombieVmCleanup.Request request) {
        String product = Utils.getProductForJobRequestClass(Vps4ZombieCleanupJobRequest.class);
        String jobGroup = Utils.getJobGroupForJobRequestClass(Vps4ZombieCleanupJobRequest.class);
        Vps4ZombieCleanupJobRequest zombieCleanupJobRequest = getJobRequestData(request.vmId, request.when);

        try {
            SchedulerJobDetail jobDetail = context.execute(
                    "Cleanup Zombie VM",
                    ctx -> schedulerService.submitJobToGroup(product, jobGroup, zombieCleanupJobRequest),
                    SchedulerJobDetail.class);
            return jobDetail.id;
        } catch (Exception e) {
            logger.error("Error while scheduling zombie cleanup job for VM: {}. Error details: {}", request.vmId, e);
            throw new RuntimeException(e);
        }
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
    }
}
