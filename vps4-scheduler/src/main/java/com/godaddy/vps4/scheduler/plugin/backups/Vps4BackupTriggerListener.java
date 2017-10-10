package com.godaddy.vps4.scheduler.plugin.backups;

import com.godaddy.vps4.scheduler.core.JobGroup;
import com.godaddy.vps4.scheduler.core.Product;
import com.godaddy.vps4.scheduler.core.SchedulerTriggerListener;
import org.quartz.JobExecutionContext;
import org.quartz.Trigger;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Product("vps4")
@JobGroup("backups")
public class Vps4BackupTriggerListener extends SchedulerTriggerListener {
    private static final Logger logger = LoggerFactory.getLogger(Vps4BackupTriggerListener.class);

    @Override
    public boolean vetoJobExecution(Trigger trigger, JobExecutionContext context) {
        logger.info("******** In vetoJobExecution *********");
        return false; // allow every job execution for now
    }
}
