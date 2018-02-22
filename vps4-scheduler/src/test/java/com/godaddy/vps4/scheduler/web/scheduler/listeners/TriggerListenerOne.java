package com.godaddy.vps4.scheduler.web.scheduler.listeners;

import com.godaddy.vps4.scheduler.core.SchedulerTriggerListener;
import com.godaddy.vps4.scheduler.core.TriggerListenerMetadata;

import org.quartz.JobExecutionContext;
import org.quartz.Trigger;

@TriggerListenerMetadata(
    product = "product1",
    jobGroup = "group1"
)
public class TriggerListenerOne extends SchedulerTriggerListener {
    @Override
    public boolean vetoJobExecution(Trigger trigger, JobExecutionContext context) {
        return false; // allow every job execution for now
    }
}
