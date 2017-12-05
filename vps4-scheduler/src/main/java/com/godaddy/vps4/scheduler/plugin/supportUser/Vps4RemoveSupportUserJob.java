package com.godaddy.vps4.scheduler.plugin.supportUser;

import java.util.UUID;

import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.godaddy.vps4.scheduler.api.plugin.Vps4RemoveSupportUserJobRequest;
import com.godaddy.vps4.scheduler.core.JobMetadata;
import com.godaddy.vps4.scheduler.core.SchedulerJob;
import com.godaddy.vps4.web.client.VmSupportUserService;
import com.godaddy.vps4.web.vm.VmAction;
import com.google.inject.Inject;

@JobMetadata(
        product = "vps4",
        jobGroup = "removeSupportUser",
        jobRequestType = Vps4RemoveSupportUserJobRequest.class
)
public class Vps4RemoveSupportUserJob extends SchedulerJob {
    private static final Logger logger = LoggerFactory.getLogger(Vps4RemoveSupportUserJob.class);

    @Inject
    VmSupportUserService vmService;

    Vps4RemoveSupportUserJobRequest request;

    @Override
    public void execute(JobExecutionContext context) throws JobExecutionException {
        try {
            removeSupportUser(request.vmId);
        }
        catch (Exception e) {
            logger.error("Error while remove support user {} from vm {}. {}", request.vmId, e);
            throw new JobExecutionException(e);
        }
    }

    private void removeSupportUser(UUID vmId) {
        logger.debug("Removing support user from vm {}.", request.vmId);
        VmAction action = vmService.removeSupportUser(vmId);
        logger.info("Remove support user from vm {} action submitted. Action: {}", request.vmId, action.id);
        
    }

    public void setRequest(Vps4RemoveSupportUserJobRequest request) {
        this.request = request;
    }
}
