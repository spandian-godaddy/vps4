package com.godaddy.vps4.orchestration.backupstorage;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.godaddy.hfs.vm.VmAction;
import com.godaddy.hfs.vm.VmService;
import com.godaddy.vps4.orchestration.ActionCommand;
import com.godaddy.vps4.orchestration.hfs.vm.WaitForVmAction;
import com.godaddy.vps4.orchestration.vm.VmActionRequest;
import com.godaddy.vps4.vm.ActionService;
import com.godaddy.vps4.backupstorage.BackupStorageService;
import com.godaddy.vps4.vm.VirtualMachine;

import gdg.hfs.orchestration.CommandContext;
import gdg.hfs.orchestration.CommandMetadata;
import gdg.hfs.orchestration.CommandRetryStrategy;

@CommandMetadata(
        name = "Vps4CreateBackupStorage",
        requestType = VmActionRequest.class,
        retryStrategy = CommandRetryStrategy.NEVER
)
public class Vps4CreateBackupStorage extends ActionCommand<VmActionRequest, Void> {
    private static final Logger logger = LoggerFactory.getLogger(Vps4CreateBackupStorage.class);
    private final BackupStorageService backupStorageService;
    private final VmService vmService;

    @Inject
    public Vps4CreateBackupStorage(ActionService actionService,
                                   BackupStorageService backupStorageService,
                                   VmService vmService) {
        super(actionService);
        this.backupStorageService = backupStorageService;
        this.vmService = vmService;
    }

    @Override
    public Void executeWithAction(CommandContext context, VmActionRequest request) {
        VirtualMachine vm = request.virtualMachine;
        logger.info("Calling HFS to create backup storage for vmId: {}, hfsVmId: {}", vm.vmId, vm.hfsVmId);

        VmAction hfsBackupStorageAction = context.execute("CreateBackupStorageHfs",
                                                          ctx -> vmService.createBackupStorage(vm.hfsVmId),
                                                          VmAction.class);
        context.execute(WaitForVmAction.class, hfsBackupStorageAction);

        // resetting the credentials is necessary to populate the username and domain fields in the DB
        backupStorageService.createBackupStorage(vm.vmId);
        context.execute(Vps4ResetBackupStorageCreds.class, request);

        return null;
    }
}
