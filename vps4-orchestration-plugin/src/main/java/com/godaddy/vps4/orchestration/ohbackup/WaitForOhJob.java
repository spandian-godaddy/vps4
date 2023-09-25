package com.godaddy.vps4.orchestration.ohbackup;

import java.util.UUID;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.godaddy.vps4.oh.jobs.OhJobService;
import com.godaddy.vps4.oh.jobs.models.OhJob;
import com.godaddy.vps4.oh.jobs.models.OhJobStatus;
import com.godaddy.vps4.orchestration.scheduler.Utils;

import gdg.hfs.orchestration.Command;
import gdg.hfs.orchestration.CommandContext;

public class WaitForOhJob implements Command<WaitForOhJob.Request, Void> {
    private static final Logger logger = LoggerFactory.getLogger(WaitForOhJob.class);

    private final OhJobService ohJobService;

    @Inject
    public WaitForOhJob(OhJobService ohJobService) {
        this.ohJobService = ohJobService;
    }

    public static class Request {
        UUID vmId;
        UUID jobId;
    }

    @Override
    public Void execute(CommandContext context, WaitForOhJob.Request request) {
        OhJob job;

        do {
            job = Utils.runWithRetriesForServerAndProcessingErrorException(context,
                                                              logger,
                                                              () -> ohJobService.getJob(request.vmId, request.jobId));
        } while (job != null && (job.status == OhJobStatus.PENDING || job.status == OhJobStatus.STARTED));

        if (job == null) {
            throw new RuntimeException("Failed to complete OH job");
        }
        if (job.status != OhJobStatus.SUCCESS) {
            throw new RuntimeException(String.format("Failed to complete OH job: %s, status: %s", job.id, job.status));
        }
        return null;
    }
}
