package com.godaddy.vps4.scheduler.plugin.backups;

import com.godaddy.vps4.scheduler.core.SchedulerTriggerListener;
import com.godaddy.vps4.scheduler.core.TriggerListenerMetadata;
import org.quartz.JobExecutionContext;
import org.quartz.Trigger;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@TriggerListenerMetadata(
    product = "vps4",
    jobGroup = "backups"
)
public class Vps4BackupTriggerListener extends SchedulerTriggerListener {
    private static final Logger logger = LoggerFactory.getLogger(Vps4BackupTriggerListener.class);

    @Override
    public boolean vetoJobExecution(Trigger trigger, JobExecutionContext context) {
        logger.info("******** In vetoJobExecution *********");
        return false; // allow every job execution for now
    }
}
