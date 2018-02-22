package com.godaddy.vps4.scheduler.plugin.supportUser;

import com.godaddy.vps4.scheduler.core.SchedulerTriggerListener;
import com.godaddy.vps4.scheduler.core.TriggerListenerMetadata;

import org.quartz.JobExecutionContext;
import org.quartz.Trigger;

@TriggerListenerMetadata(
        product = "vps4",
        jobGroup = "removeSupportUser"
)
public class Vps4RemoveSupportUserTriggerListener extends SchedulerTriggerListener {

    @Override
    public boolean vetoJobExecution(Trigger trigger, JobExecutionContext context) {
        return false; // allow every job execution for now
    }
}
