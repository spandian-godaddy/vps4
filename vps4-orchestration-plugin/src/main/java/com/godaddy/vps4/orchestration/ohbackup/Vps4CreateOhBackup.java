package com.godaddy.vps4.orchestration.ohbackup;

import com.godaddy.vps4.oh.backups.OhBackupService;
import com.godaddy.vps4.orchestration.ActionCommand;
import com.godaddy.vps4.orchestration.vm.VmActionRequest;
import com.godaddy.vps4.snapshot.SnapshotService;
import com.godaddy.vps4.vm.ActionService;
import com.godaddy.vps4.vm.VirtualMachine;
import com.google.inject.Inject;

import gdg.hfs.orchestration.CommandContext;
import gdg.hfs.orchestration.CommandMetadata;
import gdg.hfs.orchestration.CommandRetryStrategy;

@CommandMetadata(
        name="Vps4CreateOhBackup",
        requestType= VmActionRequest.class,
        retryStrategy = CommandRetryStrategy.NEVER
)
public class Vps4CreateOhBackup extends ActionCommand<VmActionRequest, Void> {
    private final OhBackupService ohBackupService;
    private final SnapshotService vps4SnapshotService;

    private CommandContext context;
    private VirtualMachine vm;

    @Inject
    public Vps4CreateOhBackup(ActionService actionService,
                              OhBackupService ohBackupService,
                              SnapshotService vps4SnapshotService) {
        super(actionService);
        this.ohBackupService = ohBackupService;
        this.vps4SnapshotService = vps4SnapshotService;
    }

    @Override
    protected Void executeWithAction(CommandContext context, VmActionRequest request) {
        this.context = context;
        this.vm = request.virtualMachine;
        /*
        if at quota limit
            oldest = the oldest backup of type
            if oldest is hfs snapshot
                mark for deprecation in db
        call oh api service to create oh backup
        if oldest is hfs snapshot
            deprecate it
        else
            delete it with oh api service
         */
        return null;
    }
}
