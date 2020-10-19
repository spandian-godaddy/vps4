package com.godaddy.vps4.scheduler.plugin.destroyVm;

import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.godaddy.vps4.scheduler.api.plugin.Vps4DestroyVmJobRequest;
import com.godaddy.vps4.scheduler.core.JobMetadata;
import com.godaddy.vps4.scheduler.core.SchedulerJob;
import com.godaddy.vps4.vm.VmAction;
import com.godaddy.vps4.web.client.VmService;
import com.google.inject.Inject;

@JobMetadata(
        product = "vps4",
        jobGroup = "destroyVm",
        jobRequestType = Vps4DestroyVmJobRequest.class
)
public class Vps4DestroyVmJob extends SchedulerJob {
    private static final Logger logger = LoggerFactory.getLogger(Vps4DestroyVmJob.class);

    @Inject VmService vmService;

    Vps4DestroyVmJobRequest request;

    @Override
    public void execute(JobExecutionContext context) throws JobExecutionException {
        try {
            logger.info("Destroying vm {}", request.vmId);
            VmAction action = vmService.destroyVm(request.vmId);
            logger.info("Vm {} deletion request submitted. Action {}, ", request.vmId, action.id);
        } catch (Exception e) {
            logger.error("Error while processing VM destroy job for vm {}. {}", request.vmId, e);
            throw new JobExecutionException(e);
        }
    }

    public void setRequest(Vps4DestroyVmJobRequest request) {
        this.request = request;
    }
}
