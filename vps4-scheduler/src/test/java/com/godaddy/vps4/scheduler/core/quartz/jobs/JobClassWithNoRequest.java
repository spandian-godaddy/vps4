package com.godaddy.vps4.scheduler.core.quartz.jobs;

import com.godaddy.vps4.scheduler.core.JobMetadata;
import com.godaddy.vps4.scheduler.core.SchedulerJob;

import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;

@JobMetadata(
    product = "product1",
    jobGroup = "group1"
)
public class JobClassWithNoRequest extends SchedulerJob {
    @Override
    public void execute(JobExecutionContext context) throws JobExecutionException {
    }
}
