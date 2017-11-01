package com.godaddy.vps4.scheduler.web.scheduler.jobs;

import com.godaddy.vps4.scheduler.api.core.JobRequest;
import com.godaddy.vps4.scheduler.core.JobMetadata;
import com.godaddy.vps4.scheduler.core.SchedulerJob;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@JobMetadata(
    product = "product1",
    jobGroup = "group2"
)

public class TestJobTwo extends SchedulerJob {
    private static final Logger logger = LoggerFactory.getLogger(TestJobTwo.class);

    JobRequest request;

    @Override
    public void execute(JobExecutionContext context) throws JobExecutionException {
    }

    public void setRequest(JobRequest request) {
        this.request = request;
    }
}
