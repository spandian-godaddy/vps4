package com.godaddy.vps4.scheduler.web.scheduler.jobs;

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

@Product("product1")
@JobGroup("group1")
public class TestJobOne extends SchedulerJob {
    private static final Logger logger = LoggerFactory.getLogger(TestJobOne.class);

    Request request;

    @Override
    public void execute(JobExecutionContext context) throws JobExecutionException {
    }

    public void setRequest(Request request) {
        this.request = request;
    }

    public static class Request extends JobRequest {
        public UUID vmId;
    }
}
