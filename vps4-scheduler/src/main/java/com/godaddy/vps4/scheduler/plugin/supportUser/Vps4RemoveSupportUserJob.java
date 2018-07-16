package com.godaddy.vps4.scheduler.plugin.supportUser;

import java.util.UUID;

import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.godaddy.vps4.scheduler.api.plugin.Vps4RemoveSupportUserJobRequest;
import com.godaddy.vps4.scheduler.core.JobMetadata;
import com.godaddy.vps4.scheduler.core.SchedulerJob;
import com.godaddy.vps4.vm.VmAction;
import com.godaddy.vps4.web.client.VmSupportUserService;
import com.google.inject.Inject;

@JobMetadata(
        product = "vps4",
        jobGroup = "removeSupportUser",
        jobRequestType = Vps4RemoveSupportUserJobRequest.class
)
public class Vps4RemoveSupportUserJob extends SchedulerJob {
    private static final Logger logger = LoggerFactory.getLogger(Vps4RemoveSupportUserJob.class);

    @Inject
    VmSupportUserService vmSupportUserService;

    Vps4RemoveSupportUserJobRequest request;

    @Override
    public void execute(JobExecutionContext context) throws JobExecutionException {
        try {
            removeSupportUsers(request.vmId, request.username);
        }
        catch (Exception e) {
            logger.error("Error while remove support user {} from vm {}. {}", request.vmId, e);
            throw new JobExecutionException(e);
        }
    }

    private void removeSupportUsers(UUID vmId, String username) {
        logger.debug("Removing support user {} from vm {}.", username, vmId);
        VmAction action = vmSupportUserService.removeSupportUsers(vmId, username);
        logger.info("Remove support user {} from vm {} action submitted. Action: {}", username, vmId, action.id);
    }

    public void setRequest(Vps4RemoveSupportUserJobRequest request) {
        this.request = request;
    }
}
