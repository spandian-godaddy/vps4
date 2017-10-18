package com.godaddy.vps4.scheduler.core;

import java.time.Instant;
import java.util.UUID;

public class SchedulerJobDetail {
    public final UUID id;
    public final Instant nextRun;
    public final JobRequest jobRequest;

    public SchedulerJobDetail(UUID id, Instant nextRun, JobRequest jobRequest) {
        this.id = id;
        this.nextRun = nextRun;
        this.jobRequest = jobRequest;
    }

    @Override
    public String toString() {
        return String.format("Scheduler job detail [id=%s, nextRun=%s, jobRequest=%s]", id, nextRun, jobRequest.toString());
    }
}
