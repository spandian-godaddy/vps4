package com.godaddy.vps4.orchestration.vm;

import java.util.List;
import java.util.UUID;

import com.godaddy.vps4.scheduledJob.ScheduledJob;
import com.godaddy.vps4.scheduledJob.ScheduledJobService;
import com.google.inject.Inject;

import gdg.hfs.orchestration.CommandMetadata;
import gdg.hfs.orchestration.CommandRetryStrategy;

@CommandMetadata(
        name="Vps4DeleteAllScheduledZombieJobsForVm",
        requestType= UUID.class,
        retryStrategy = CommandRetryStrategy.NEVER
)
public class Vps4DeleteAllScheduledZombieJobsForVm extends Vps4DeleteAllScheduledJobsForVm {
    private final ScheduledJobService scheduledJobService;

    @Inject
    public Vps4DeleteAllScheduledZombieJobsForVm(ScheduledJobService scheduledJobService) {
        super(scheduledJobService);
        this.scheduledJobService = scheduledJobService;
    }

    @Override
    protected List<ScheduledJob> getJobsForDeletion() {
        return scheduledJobService.getScheduledJobsByType(vmId, ScheduledJob.ScheduledJobType.ZOMBIE);
    }
}
