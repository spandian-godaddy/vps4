package com.godaddy.vps4.orchestration.vm;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.godaddy.hfs.vm.VmAction;
import com.godaddy.hfs.vm.VmService;
import com.godaddy.vps4.orchestration.ActionCommand;
import com.godaddy.vps4.orchestration.hfs.vm.WaitForVmAction;
import com.godaddy.vps4.vm.ActionService;
import com.godaddy.vps4.vm.VirtualMachine;

import gdg.hfs.orchestration.CommandContext;
import gdg.hfs.orchestration.CommandMetadata;
import gdg.hfs.orchestration.CommandRetryStrategy;

@CommandMetadata(
        name = "Vps4DestroyBackupStorage",
        requestType = VmActionRequest.class,
        responseType = Void.class,
        retryStrategy = CommandRetryStrategy.NEVER
)
public class Vps4DestroyBackupStorage extends ActionCommand<VmActionRequest, Void> {
    private static final Logger logger = LoggerFactory.getLogger(Vps4DestroyBackupStorage.class);
    private final VmService vmService;

    @Inject
    public Vps4DestroyBackupStorage(ActionService actionService, VmService vmService) {
        super(actionService);
        this.vmService = vmService;
    }

    @Override
    public Void executeWithAction(CommandContext context, VmActionRequest request) {
        VirtualMachine vm = request.virtualMachine;
        logger.info("Calling HFS to destroy backup storage for vmId: {}, hfsVmId: {}", vm.vmId, vm.hfsVmId);

        VmAction hfsBackupStorageAction = context.execute("DestroyBackupStorageHfs",
                                                        ctx -> vmService.destroyBackupStorage(vm.hfsVmId),
                                                        VmAction.class);
        context.execute(WaitForVmAction.class, hfsBackupStorageAction);

        return null;
    }
}
