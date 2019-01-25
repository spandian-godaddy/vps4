package com.godaddy.vps4.web.vm;
import com.godaddy.vps4.scheduledJob.ScheduledJob;
import com.godaddy.vps4.scheduler.api.core.SchedulerJobDetail;

public class SnapshotSchedule {

    public ScheduledJob.ScheduledJobType scheduledJobType;
    public SchedulerJobDetail schedulerJobDetail;

    public SnapshotSchedule(){}
    public SnapshotSchedule(ScheduledJob.ScheduledJobType scheduledJobType, SchedulerJobDetail schedulerJobDetail){
        this.scheduledJobType = scheduledJobType;
        this.schedulerJobDetail = schedulerJobDetail;
    }
}
