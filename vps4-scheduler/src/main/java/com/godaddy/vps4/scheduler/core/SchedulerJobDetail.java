package com.godaddy.vps4.scheduler.core;

import java.time.Instant;
import java.util.UUID;

public class SchedulerJobDetail {
    public final UUID id;
    public final Instant when;

    public SchedulerJobDetail(UUID id, Instant when) {
        this.id = id;
        this.when = when;
    }

    @Override
    public String toString() {
        return String.format("Scheduler job detail [id=%s, when=%s]", id, when);
    }
}
