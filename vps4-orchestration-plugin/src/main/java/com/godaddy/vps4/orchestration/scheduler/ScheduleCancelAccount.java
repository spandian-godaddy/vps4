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
import com.godaddy.vps4.scheduler.api.plugin.Vps4CancelAccountJobRequest;
import com.godaddy.vps4.scheduler.api.web.SchedulerWebService;

import gdg.hfs.orchestration.Command;
import gdg.hfs.orchestration.CommandContext;

public class ScheduleCancelAccount implements Command<UUID, UUID> {
    private static final Logger logger = LoggerFactory.getLogger(ScheduleCancelAccount.class);
    private CommandContext context;

    private final SchedulerWebService schedulerWebService;
    private final Config config;

    @Inject
    public ScheduleCancelAccount(SchedulerWebService schedulerWebService, Config config) {
        this.schedulerWebService = schedulerWebService;
        this.config = config;
    }

    @Override
    public UUID execute(CommandContext context, UUID vmId) {
        this.context = context;

        String product = Utils.getProductForJobRequestClass(Vps4CancelAccountJobRequest.class);
        String jobGroup = Utils.getJobGroupForJobRequestClass(Vps4CancelAccountJobRequest.class);
        Vps4CancelAccountJobRequest request = getJobRequest(vmId);

        try {
            SchedulerJobDetail jobDetail = context.execute(
                    "Retry Cancel Account",
                    ctx -> schedulerWebService.submitJobToGroup(product, jobGroup, request),
                    SchedulerJobDetail.class);
            recordJobId(request.vmId, jobDetail.id);
            return jobDetail.id;
        } catch (Exception e) {
            logger.error("Error while rescheduling cancel account job for VM: {}. Error details: {}", request.vmId, e);
            throw new RuntimeException(e);
        }
    }

    private Vps4CancelAccountJobRequest getJobRequest(UUID vmId) {
        Vps4CancelAccountJobRequest request = new Vps4CancelAccountJobRequest();
        request.vmId = vmId;
        request.jobType = JobType.ONE_TIME;
        int waitTime = Integer.parseInt(config.get("vps4.cancelAccount.retryWaitHours"));
        request.when = Instant.now().plus(waitTime, ChronoUnit.HOURS);
        return request;
    }

    private void recordJobId(UUID vmId, UUID jobId) {
        Vps4RecordScheduledJobForVm.Request request = new Vps4RecordScheduledJobForVm.Request();
        request.jobId = jobId;
        request.vmId = vmId;
        request.jobType = ScheduledJobType.CANCEL_ACCOUNT;
        context.execute("RecordScheduledJobId", Vps4RecordScheduledJobForVm.class, request);
    }
}
