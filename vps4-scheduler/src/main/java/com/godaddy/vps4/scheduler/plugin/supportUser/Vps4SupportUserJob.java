package com.godaddy.vps4.scheduler.plugin.supportUser;

import com.godaddy.vps4.scheduler.api.plugin.Vps4SupportUserJobRequest;
import com.godaddy.vps4.scheduler.core.JobMetadata;
import com.godaddy.vps4.scheduler.core.SchedulerJob;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.quartz.JobKey;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@JobMetadata(
        product = "vps4",
        jobGroup = "supportUser",
        jobRequestType = Vps4SupportUserJobRequest.class
)
public class Vps4SupportUserJob extends SchedulerJob {
    private static final Logger logger = LoggerFactory.getLogger(Vps4SupportUserJob.class);

    Vps4SupportUserJobRequest request;

    @Override
    public void execute(JobExecutionContext context) throws JobExecutionException {
        logger.info("******** Executing support user job *********");
        JobKey key = context.getJobDetail().getKey();

        try {
            logger.info("*********** request data: {} **********", this.request.vmId);
            logger.info("*********** request data: {} **********", this.request.supportUserName);
            logger.info("*********** doing work ***********");
        }
        catch (Exception e) {
            logger.error("Error while processing support user job ({}) for request: {}", key, request);
        }
    }

    public void setRequest(Vps4SupportUserJobRequest request) {
        this.request = request;
    }
}
