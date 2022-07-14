package com.godaddy.vps4.oh.jobs;

import java.util.UUID;

import com.godaddy.vps4.oh.jobs.models.OhJob;

public interface OhJobService {
    OhJob getJob(UUID vmId, UUID jobId);
}
