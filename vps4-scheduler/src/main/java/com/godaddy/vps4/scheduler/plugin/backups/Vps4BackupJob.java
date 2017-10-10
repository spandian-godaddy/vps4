package com.godaddy.vps4.scheduler.plugin.backups;

import com.godaddy.vps4.scheduler.core.JobGroup;
import com.godaddy.vps4.scheduler.core.JobRequest;
import com.godaddy.vps4.scheduler.core.Product;
import com.godaddy.vps4.scheduler.core.SchedulerJob;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.quartz.JobKey;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.UUID;

@Product("vps4")
@JobGroup("backups")
public class Vps4BackupJob extends SchedulerJob {
    private static final Logger logger = LoggerFactory.getLogger(Vps4BackupJob.class);

    Request request;

    @Override
    public void execute(JobExecutionContext context) throws JobExecutionException {
        logger.info("******** Executing backup job *********");
        JobKey key = context.getJobDetail().getKey();

        try {
            logger.info("*********** request data: {} **********", this.request.vmId);
            logger.info("*********** doing work ***********");
        }
        catch (Exception e) {
            logger.error("Error while processing backup job ({}) for request: {}", key, request);
        }
    }

    public void setRequest(Request request) {
        this.request = request;
    }

    public static class Request extends JobRequest {
        public UUID vmId;
    }
}
