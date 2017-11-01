package com.godaddy.vps4.scheduler.core.quartz.jobs;

import com.godaddy.vps4.scheduler.core.JobMetadata;
import com.godaddy.vps4.scheduler.core.SchedulerJob;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@JobMetadata(
    product = "product1",
    jobGroup = "group1"
)
public class JobClassWithNoRequest extends SchedulerJob {
    private static final Logger logger = LoggerFactory.getLogger(JobClassWithNoRequest.class);

    @Override
    public void execute(JobExecutionContext context) throws JobExecutionException {
    }
}
