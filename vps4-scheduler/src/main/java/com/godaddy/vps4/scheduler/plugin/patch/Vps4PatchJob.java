package com.godaddy.vps4.scheduler.plugin.patch;

import com.godaddy.vps4.scheduler.api.plugin.Vps4PatchJobRequest;
import com.godaddy.vps4.scheduler.core.JobMetadata;
import com.godaddy.vps4.scheduler.core.SchedulerJob;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.quartz.JobKey;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@JobMetadata(
        product = "vps4",
        jobGroup = "patch",
        jobRequestType = Vps4PatchJobRequest.class
)
public class Vps4PatchJob extends SchedulerJob {
    private static final Logger logger = LoggerFactory.getLogger(Vps4PatchJob.class);

    Vps4PatchJobRequest request;

    @Override
    public void execute(JobExecutionContext context) throws JobExecutionException {
        logger.info("******** Executing patch job *********");
        JobKey key = context.getJobDetail().getKey();

        try {
            logger.info("*********** request data: {} **********", this.request.vmId);
            logger.info("*********** doing work ***********");
        }
        catch (Exception e) {
            logger.error("Error while processing patch job ({}) for request: {}", key, request);
        }
    }

    public void setRequest(Vps4PatchJobRequest request) {
        this.request = request;
    }
}
