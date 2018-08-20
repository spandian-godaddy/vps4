package com.godaddy.vps4.orchestration.vm;

import com.godaddy.vps4.scheduledJob.ScheduledJob;
import com.godaddy.vps4.scheduledJob.ScheduledJobService;
import com.google.inject.Inject;
import gdg.hfs.orchestration.Command;
import gdg.hfs.orchestration.CommandContext;
import gdg.hfs.orchestration.CommandMetadata;
import gdg.hfs.orchestration.CommandRetryStrategy;

import java.util.UUID;

@CommandMetadata(
            name="Vps4RecordScheduledJobForVm",
            requestType=Vps4RecordScheduledJobForVm.Request.class,
            retryStrategy = CommandRetryStrategy.NEVER
)
public class Vps4RecordScheduledJobForVm implements Command<Vps4RecordScheduledJobForVm.Request, Void> {

    private final ScheduledJobService scheduledJobService;

    @Inject
    public Vps4RecordScheduledJobForVm(ScheduledJobService scheduledJobService) {
        this.scheduledJobService = scheduledJobService;
    }

    @Override
    public Void execute(CommandContext context, Request request) {
        scheduledJobService.insertScheduledJob(request.jobId, request.vmId, request.jobType);
        return null;
    }

    public static class Request {
        public UUID vmId;
        public UUID jobId;
        public ScheduledJob.ScheduledJobType jobType;
    }
}
