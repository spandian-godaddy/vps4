package com.godaddy.vps4.orchestration.vm;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.godaddy.vps4.network.IpAddress;
import com.godaddy.vps4.network.NetworkService;
import com.godaddy.vps4.orchestration.backupstorage.Vps4DestroyBackupStorage;
import com.godaddy.vps4.vm.ActionService;

import gdg.hfs.orchestration.CommandMetadata;
import gdg.hfs.orchestration.CommandRetryStrategy;

@CommandMetadata(
        name = "Vps4DestroyDedicated",
        requestType = VmActionRequest.class,
        responseType = Vps4DestroyDedicated.Response.class,
        retryStrategy = CommandRetryStrategy.NEVER
)
public class Vps4DestroyDedicated extends Vps4DestroyVm {
    private static final Logger logger = LoggerFactory.getLogger(Vps4DestroyDedicated.class);

    @Inject
    public Vps4DestroyDedicated(ActionService actionService, NetworkService networkService) {
        super(actionService, networkService);
    }

    @Override
    protected void destroyBackupStorage() {
        try {
            VmActionRequest request = new VmActionRequest();
            request.virtualMachine = vm;
            context.execute(Vps4DestroyBackupStorage.class, request);
        } catch (RuntimeException e) {
            // do nothing, this is likely because a backup space does not exist for this server
            logger.warn("Could not destroy backup storage for VM ID {}", vm.vmId);
        }
    }

    @Override
    protected void removeIpFromServer(IpAddress address) {
        // Ded4 servers do not need to unbind and release IPs back to an IP Pool
    }
}
