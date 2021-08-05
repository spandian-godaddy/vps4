package com.godaddy.vps4.scheduler.plugin.cancelAccount;

import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.godaddy.vps4.scheduler.api.plugin.Vps4CancelAccountJobRequest;
import com.godaddy.vps4.scheduler.core.JobMetadata;
import com.godaddy.vps4.scheduler.core.SchedulerJob;
import com.godaddy.vps4.vm.VmAction;
import com.godaddy.vps4.web.client.VmService;
import com.google.inject.Inject;

@JobMetadata(
        product = "vps4",
        jobGroup = "cancelAccount",
        jobRequestType = Vps4CancelAccountJobRequest.class
)
public class Vps4CancelAccountJob extends SchedulerJob {
    private static final Logger logger = LoggerFactory.getLogger(Vps4CancelAccountJob.class);

    @Inject VmService vmService;

    Vps4CancelAccountJobRequest request;

    @Override
    public void execute(JobExecutionContext context) throws JobExecutionException {
        try {
            logger.info("Cancelling account {}", request.vmId);
            VmAction action = vmService.zombie(request.vmId);
            logger.info("Cancel {} account request submitted. Action {}, ", request.vmId, action.id);
        } catch (Exception e) {
            logger.error("Error while processing cancel account job for vm {}. {}", request.vmId, e);
            throw new JobExecutionException(e);
        }
    }

    public void setRequest(Vps4CancelAccountJobRequest request) {
        this.request = request;
    }
}
