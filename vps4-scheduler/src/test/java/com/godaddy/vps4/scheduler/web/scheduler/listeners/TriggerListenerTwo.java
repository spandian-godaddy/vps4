package com.godaddy.vps4.scheduler.web.scheduler.listeners;

import com.godaddy.vps4.scheduler.core.SchedulerTriggerListener;
import com.godaddy.vps4.scheduler.core.TriggerListenerMetadata;
import org.quartz.JobExecutionContext;
import org.quartz.Trigger;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@TriggerListenerMetadata(
        product = "product1",
        jobGroup = "group2"
)
public class TriggerListenerTwo extends SchedulerTriggerListener {
    private static final Logger logger = LoggerFactory.getLogger(TriggerListenerTwo.class);

    @Override
    public boolean vetoJobExecution(Trigger trigger, JobExecutionContext context) {
        return false; // allow every job execution for now
    }
}
