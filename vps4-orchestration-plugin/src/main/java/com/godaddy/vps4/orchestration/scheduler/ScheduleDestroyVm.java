package com.godaddy.vps4.orchestration.scheduler;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.godaddy.hfs.config.Config;
import com.godaddy.vps4.orchestration.vm.Vps4RecordScheduledJobForVm;
import com.godaddy.vps4.scheduledJob.ScheduledJob.ScheduledJobType;
import com.godaddy.vps4.scheduler.api.core.JobType;
import com.godaddy.vps4.scheduler.api.core.SchedulerJobDetail;
import com.godaddy.vps4.scheduler.api.core.utils.Utils;
import com.godaddy.vps4.scheduler.api.plugin.Vps4DestroyVmJobRequest;
import com.godaddy.vps4.scheduler.api.web.SchedulerWebService;

import gdg.hfs.orchestration.Command;
import gdg.hfs.orchestration.CommandContext;

public class ScheduleDestroyVm implements Command<UUID, UUID> {
    private static final Logger logger = LoggerFactory.getLogger(ScheduleDestroyVm.class);
    private CommandContext context;

    private final SchedulerWebService schedulerWebService;
    private final Config config;

    @Inject
    public ScheduleDestroyVm(SchedulerWebService schedulerWebService, Config config) {
        this.schedulerWebService = schedulerWebService;
        this.config = config;
    }

    @Override
    public UUID execute(CommandContext context, UUID vmId) {
        this.context = context;

        String product = Utils.getProductForJobRequestClass(Vps4DestroyVmJobRequest.class);
        String jobGroup = Utils.getJobGroupForJobRequestClass(Vps4DestroyVmJobRequest.class);
        Vps4DestroyVmJobRequest request = getJobRequest(vmId);

        try {
            SchedulerJobDetail jobDetail = context.execute(
                    "Retry Destroy VM",
                    ctx -> schedulerWebService.submitJobToGroup(product, jobGroup, request),
                    SchedulerJobDetail.class);
            recordJobId(request.vmId, jobDetail.id);
            return jobDetail.id;
        } catch (Exception e) {
            logger.error("Error while rescheduling destroy vm job for VM: {}. Error details: {}", request.vmId, e);
            throw new RuntimeException(e);
        }
    }

    private Vps4DestroyVmJobRequest getJobRequest(UUID vmId) {
        Vps4DestroyVmJobRequest request = new Vps4DestroyVmJobRequest();
        request.vmId = vmId;
        request.jobType = JobType.ONE_TIME;
        int waitTime = Integer.parseInt(config.get("vps4.destroyVm.retryWaitHours"));
        request.when = Instant.now().plus(waitTime, ChronoUnit.HOURS);
        return request;
    }

    private void recordJobId(UUID vmId, UUID jobId) {
        Vps4RecordScheduledJobForVm.Request request = new Vps4RecordScheduledJobForVm.Request();
        request.jobId = jobId;
        request.vmId = vmId;
        request.jobType = ScheduledJobType.DESTROY_VM;
        context.execute("RecordScheduledJobId", Vps4RecordScheduledJobForVm.class, request);
    }
}
