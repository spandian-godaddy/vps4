package com.godaddy.vps4.scheduler.web.scheduler.jobs;

import com.godaddy.vps4.scheduler.api.core.JobRequest;
import com.godaddy.vps4.scheduler.core.JobMetadata;
import com.godaddy.vps4.scheduler.core.SchedulerJob;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;

@JobMetadata(
    product = "product2",
    jobGroup = "group1"
)
public class TestJobThree extends SchedulerJob {
    JobRequest request;

    @Override
    public void execute(JobExecutionContext context) throws JobExecutionException {
    }

    public void setRequest(JobRequest request) {
        this.request = request;
    }
}
