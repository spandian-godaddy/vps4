package com.godaddy.vps4.orchestration.nydus;

import com.godaddy.vps4.orchestration.ActionCommand;
import com.godaddy.vps4.orchestration.Vps4ActionRequest;
import com.godaddy.vps4.orchestration.hfs.sysadmin.WaitForSysAdminAction;
import com.godaddy.vps4.vm.ActionService;
import gdg.hfs.orchestration.CommandContext;
import gdg.hfs.orchestration.CommandMetadata;
import gdg.hfs.orchestration.CommandRetryStrategy;
import com.godaddy.hfs.sysadmin.SysAdminService;
import com.godaddy.hfs.sysadmin.SysAdminAction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.util.UUID;

@CommandMetadata(
        name = "UpgradeNydus",
        requestType = UpgradeNydus.Request.class,
        retryStrategy = CommandRetryStrategy.NEVER
)
public class UpgradeNydus extends ActionCommand<UpgradeNydus.Request, Void> {
    private static final Logger logger = LoggerFactory.getLogger(UpgradeNydus.class);

    public static class Request extends Vps4ActionRequest {
        public UUID vmId;
        public long hfsVmId;
        public String version;
    }

    private final SysAdminService sysAdminService;

    @Inject
    public UpgradeNydus(ActionService actionService, SysAdminService sysAdminService) {
        super(actionService);
        this.sysAdminService = sysAdminService;
    }

    @Override
    public Void executeWithAction(CommandContext context, UpgradeNydus.Request request) {
        logger.debug("Upgrading Nydus for VM ID {} ", request.vmId);

        SysAdminAction hfsSysAction = context.execute("UpgradeNydus",
                ctx -> sysAdminService.updateNydus(request.hfsVmId, "upgrade", request.version),
                SysAdminAction.class);
        context.execute("WaitForNydusUpgrade-" + request.hfsVmId, WaitForSysAdminAction.class, hfsSysAction);

        return null;
    }
}
