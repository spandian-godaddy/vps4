package com.godaddy.vps4.orchestration.scheduler;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.godaddy.hfs.config.Config;
import com.godaddy.vps4.scheduler.api.core.JobType;
import com.godaddy.vps4.scheduler.api.core.SchedulerJobDetail;
import com.godaddy.vps4.scheduler.api.core.utils.Utils;
import com.godaddy.vps4.scheduler.api.plugin.Vps4RemoveSupportUserJobRequest;
import com.godaddy.vps4.scheduler.api.web.SchedulerWebService;

import gdg.hfs.orchestration.Command;
import gdg.hfs.orchestration.CommandContext;

public class ScheduleSupportUserRemoval implements Command<ScheduleSupportUserRemoval.Request, UUID> {

    private static final Logger logger = LoggerFactory.getLogger(ScheduleSupportUserRemoval.class);

    private final SchedulerWebService schedulerWebService;
    private final Config config;

    @Inject
    public ScheduleSupportUserRemoval(SchedulerWebService schedulerWebService, Config config) {
        this.schedulerWebService = schedulerWebService;
        this.config = config;
    }

    @Override
    public UUID execute(CommandContext context, ScheduleSupportUserRemoval.Request request) {
        String product = Utils.getProductForJobRequestClass(Vps4RemoveSupportUserJobRequest.class);
        String jobGroup = Utils.getJobGroupForJobRequestClass(Vps4RemoveSupportUserJobRequest.class);
        Vps4RemoveSupportUserJobRequest removeSupportUserRequest = getJobRequest(request);

        try {
            SchedulerJobDetail jobDetail = context.execute("Create schedule",
                    ctx -> schedulerWebService.submitJobToGroup(product, jobGroup, removeSupportUserRequest), SchedulerJobDetail.class);
            return jobDetail.id;
        } catch (Exception e) {
            logger.error("Error while creating a scheduled job to remove support user from VM: {}. Error details: {}", request.vmId, e);
            throw new RuntimeException(e);
        }
    }

    private Vps4RemoveSupportUserJobRequest getJobRequest(Request request) {
        Vps4RemoveSupportUserJobRequest userRequest = new Vps4RemoveSupportUserJobRequest();
        userRequest.vmId = request.vmId;
        userRequest.jobType = JobType.ONE_TIME;
        int waitTime = Integer.parseInt(config.get("vps4.setupUser.removalWaitHours"));
        userRequest.when = Instant.now().plus(waitTime, ChronoUnit.HOURS);
        return userRequest;
    }

    public static class Request {
        public UUID vmId;
    }
}
