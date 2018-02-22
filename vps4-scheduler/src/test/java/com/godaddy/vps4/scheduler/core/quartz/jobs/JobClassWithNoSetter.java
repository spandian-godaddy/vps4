package com.godaddy.vps4.scheduler.core.quartz.jobs;

import com.godaddy.vps4.scheduler.core.JobMetadata;
import com.godaddy.vps4.scheduler.core.SchedulerJob;

import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;

@JobMetadata(
    product = "product1",
    jobGroup = "group1",
    jobRequestType = JobRequestOne.class
)
public class JobClassWithNoSetter extends SchedulerJob {
    JobRequestOne request;

    @Override
    public void execute(JobExecutionContext context) throws JobExecutionException {
    }
}
