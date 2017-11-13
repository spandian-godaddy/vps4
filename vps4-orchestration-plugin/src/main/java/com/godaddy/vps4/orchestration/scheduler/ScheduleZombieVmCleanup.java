package com.godaddy.vps4.orchestration.scheduler;

import com.godaddy.hfs.config.Config;
import com.godaddy.vps4.scheduler.api.core.JobType;
import com.godaddy.vps4.scheduler.api.core.SchedulerJobDetail;
import com.godaddy.vps4.scheduler.api.core.utils.Utils;
import com.godaddy.vps4.scheduler.api.plugin.Vps4ZombieCleanupJobRequest;
import com.godaddy.vps4.scheduler.api.web.SchedulerWebService;
import gdg.hfs.orchestration.Command;
import gdg.hfs.orchestration.CommandContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

public class ScheduleZombieVmCleanup implements Command<UUID, UUID> {

    private static final Logger logger = LoggerFactory.getLogger(ScheduleZombieVmCleanup.class);

    private final SchedulerWebService schedulerService;
    private final Config config;

    @Inject
    public ScheduleZombieVmCleanup(SchedulerWebService schedulerService, Config config) {
        this.schedulerService = schedulerService;
        this.config = config;
    }

    @Override
    public UUID execute(CommandContext context, UUID vmId) {
        String product = Utils.getProductForJobRequestClass(Vps4ZombieCleanupJobRequest.class);
        String jobGroup = Utils.getJobGroupForJobRequestClass(Vps4ZombieCleanupJobRequest.class);
        Vps4ZombieCleanupJobRequest zombieCleanupJobRequest = getJobRequestData(vmId);

        try {
            SchedulerJobDetail jobDetail = context.execute(
                    "Cleanup Zombie VM",
                    ctx -> schedulerService.submitJobToGroup(product, jobGroup, zombieCleanupJobRequest),
                    SchedulerJobDetail.class);
            return jobDetail.id;
        } catch (Exception e) {
            logger.error("Error while scheduling zombie cleanup job for VM: {}. Error details: {}", vmId, e);
            throw new RuntimeException(e);
        }
    }

    private Vps4ZombieCleanupJobRequest getJobRequestData(UUID vmId) {
        Vps4ZombieCleanupJobRequest zombieCleanupRequest = new Vps4ZombieCleanupJobRequest();
        zombieCleanupRequest.vmId = vmId;
        zombieCleanupRequest.jobType = JobType.ONE_TIME;
        int waitTime = Integer.valueOf(config.get("vps4.zombie.cleanup.waittime"));
        zombieCleanupRequest.when = Instant.now().plus(waitTime, ChronoUnit.DAYS);
        return zombieCleanupRequest;
    }
}
