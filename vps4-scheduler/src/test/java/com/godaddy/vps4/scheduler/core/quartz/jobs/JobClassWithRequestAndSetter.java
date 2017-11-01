package com.godaddy.vps4.scheduler.core.quartz.jobs;

import com.godaddy.vps4.scheduler.core.JobMetadata;
import com.godaddy.vps4.scheduler.core.SchedulerJob;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@JobMetadata(
        product = "product1",
        jobGroup = "group1",
        jobRequestType = JobRequestOne.class
)
public class JobClassWithRequestAndSetter extends SchedulerJob {
    private static final Logger logger = LoggerFactory.getLogger(JobClassWithRequestAndSetter.class);

    JobRequestOne request;

    @Override
    public void execute(JobExecutionContext context) throws JobExecutionException {
    }

    public void setRequest(JobRequestOne request) {
        this.request = request;
    }
}
