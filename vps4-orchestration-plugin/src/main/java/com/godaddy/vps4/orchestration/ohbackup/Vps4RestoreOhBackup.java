package com.godaddy.vps4.orchestration.ohbackup;

import java.util.UUID;

import com.godaddy.vps4.oh.backups.OhBackupService;
import com.godaddy.vps4.oh.jobs.models.OhJob;
import com.godaddy.vps4.orchestration.ActionCommand;
import com.godaddy.vps4.orchestration.vm.VmActionRequest;
import com.godaddy.vps4.vm.ActionService;
import com.google.inject.Inject;

import gdg.hfs.orchestration.CommandContext;
import gdg.hfs.orchestration.CommandMetadata;
import gdg.hfs.orchestration.CommandRetryStrategy;

@CommandMetadata(
        name="Vps4RestoreOhBackup",
        requestType= Vps4RestoreOhBackup.Request.class,
        retryStrategy = CommandRetryStrategy.NEVER
)
public class Vps4RestoreOhBackup extends ActionCommand<Vps4RestoreOhBackup.Request, Void> {
    private final OhBackupService ohBackupService;

    @Inject
    public Vps4RestoreOhBackup(ActionService actionService, OhBackupService ohBackupService) {
        super(actionService);
        this.ohBackupService = ohBackupService;
    }

    public static class Request extends VmActionRequest {
        public UUID backupId;
    }

    @Override
    protected Void executeWithAction(CommandContext context, Vps4RestoreOhBackup.Request request) {
        OhJob job = context.execute("RestoreOhBackup",
                                    ctx -> ohBackupService.restoreBackup(request.virtualMachine.vmId, request.backupId),
                                    OhJob.class);
        WaitForOhJob.Request waitRequest = new WaitForOhJob.Request();
        waitRequest.vmId = request.virtualMachine.vmId;
        waitRequest.jobId = job.id;
        context.execute(WaitForOhJob.class, waitRequest);
        return null;
    }
}
