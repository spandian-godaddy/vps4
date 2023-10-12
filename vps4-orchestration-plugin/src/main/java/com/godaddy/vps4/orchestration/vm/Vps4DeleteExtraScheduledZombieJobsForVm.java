package com.godaddy.vps4.orchestration.vm;

import com.godaddy.vps4.orchestration.vm.Vps4DeleteAllScheduledJobsForVm;
import com.godaddy.vps4.scheduledJob.ScheduledJob;
import com.godaddy.vps4.scheduledJob.ScheduledJobService;
import com.google.inject.Inject;
import gdg.hfs.orchestration.CommandMetadata;
import gdg.hfs.orchestration.CommandRetryStrategy;

import java.util.List;
import java.util.UUID;

@CommandMetadata(
        name="Vps4DeleteExtraScheduledZombieJobsForVm",
        requestType= UUID.class,
        retryStrategy = CommandRetryStrategy.NEVER
)
public class Vps4DeleteExtraScheduledZombieJobsForVm extends Vps4DeleteAllScheduledJobsForVm {
    private final ScheduledJobService scheduledJobService;

    @Inject
    public Vps4DeleteExtraScheduledZombieJobsForVm(ScheduledJobService scheduledJobService) {
        super(scheduledJobService);
        this.scheduledJobService = scheduledJobService;
    }

    @Override
    protected List<ScheduledJob> getJobsForDeletion() {
        List<ScheduledJob> jobs = scheduledJobService.getScheduledJobsByType(vmId, ScheduledJob.ScheduledJobType.ZOMBIE);
        return jobs.subList(1, jobs.size());
    }
}
