package com.godaddy.vps4.web.vm;

import java.time.Instant;
import java.util.UUID;

public class ScheduledZombieCleanupJob {
    public UUID jobId;
    public Instant nextRun;
    public boolean isPaused;
}
