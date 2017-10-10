package com.godaddy.vps4.scheduler.web.scheduler.listeners;

import com.godaddy.vps4.scheduler.core.JobGroup;
import com.godaddy.vps4.scheduler.core.Product;
import com.godaddy.vps4.scheduler.core.SchedulerTriggerListener;
import org.quartz.JobExecutionContext;
import org.quartz.Trigger;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Product("product1")
@JobGroup("group2")
public class TriggerListenerTwo extends SchedulerTriggerListener {
    private static final Logger logger = LoggerFactory.getLogger(TriggerListenerTwo.class);

    @Override
    public boolean vetoJobExecution(Trigger trigger, JobExecutionContext context) {
        return false; // allow every job execution for now
    }
}
