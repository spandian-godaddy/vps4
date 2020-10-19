package com.godaddy.vps4.scheduler.plugin.destroyVm;

import org.quartz.JobExecutionContext;
import org.quartz.Trigger;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.godaddy.vps4.scheduler.core.SchedulerTriggerListener;
import com.godaddy.vps4.scheduler.core.TriggerListenerMetadata;

@TriggerListenerMetadata(
        product = "vps4",
        jobGroup = "destroyVm"
)
public class Vps4DestroyVmTriggerListener extends SchedulerTriggerListener {
    private static final Logger logger = LoggerFactory.getLogger(Vps4DestroyVmTriggerListener.class);

    @Override
    public boolean vetoJobExecution(Trigger trigger, JobExecutionContext context) {
        logger.info("******** In vetoJobExecution *********");
        return false; // allow every job execution for now
    }
}
