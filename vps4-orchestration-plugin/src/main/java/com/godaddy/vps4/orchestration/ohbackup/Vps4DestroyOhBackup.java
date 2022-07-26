package com.godaddy.vps4.orchestration.ohbackup;

import java.util.UUID;

import com.godaddy.vps4.oh.OhBackupDataService;
import com.godaddy.vps4.oh.backups.OhBackupService;
import com.godaddy.vps4.orchestration.ActionCommand;
import com.godaddy.vps4.orchestration.vm.VmActionRequest;
import com.godaddy.vps4.vm.ActionService;
import com.google.inject.Inject;

import gdg.hfs.orchestration.CommandContext;
import gdg.hfs.orchestration.CommandMetadata;
import gdg.hfs.orchestration.CommandRetryStrategy;

@CommandMetadata(
        name="Vps4DestroyOhBackup",
        requestType= Vps4DestroyOhBackup.Request.class,
        retryStrategy = CommandRetryStrategy.NEVER
)
public class Vps4DestroyOhBackup extends ActionCommand<Vps4DestroyOhBackup.Request, Void> {
    private final OhBackupService ohBackupService;
    private final OhBackupDataService ohBackupDataService;

    @Inject
    public Vps4DestroyOhBackup(ActionService actionService,
                               OhBackupService ohBackupService,
                               OhBackupDataService ohBackupDataService) {
        super(actionService);
        this.ohBackupService = ohBackupService;
        this.ohBackupDataService = ohBackupDataService;
    }

    public static class Request extends VmActionRequest {
        public UUID backupId;
    }

    @Override
    protected Void executeWithAction(CommandContext context, Vps4DestroyOhBackup.Request request) {
        ohBackupService.deleteBackup(request.virtualMachine.vmId, request.backupId);
        ohBackupDataService.destroyBackup(request.backupId);
        return null;
    }
}
