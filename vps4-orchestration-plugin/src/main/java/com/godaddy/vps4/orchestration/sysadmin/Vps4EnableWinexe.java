package com.godaddy.vps4.orchestration.sysadmin;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.godaddy.vps4.orchestration.ActionCommand;
import com.godaddy.vps4.orchestration.hfs.sysadmin.WaitForSysAdminAction;
import com.godaddy.vps4.orchestration.vm.VmActionRequest;
import com.godaddy.vps4.vm.ActionService;

import gdg.hfs.orchestration.CommandContext;
import gdg.hfs.orchestration.CommandMetadata;
import gdg.hfs.orchestration.CommandRetryStrategy;
import com.godaddy.hfs.sysadmin.SysAdminService;
import com.godaddy.hfs.sysadmin.SysAdminAction;

@CommandMetadata(
        name = "Vps4EnableWinexe",
        requestType = VmActionRequest.class,
        retryStrategy = CommandRetryStrategy.NEVER
)
public class Vps4EnableWinexe extends ActionCommand<VmActionRequest, Void> {
    private static final Logger logger = LoggerFactory.getLogger(Vps4EnableWinexe.class);

    private final SysAdminService sysAdminService;

    @Inject
    public Vps4EnableWinexe(ActionService actionService, SysAdminService sysAdminService) {
        super(actionService);
        this.sysAdminService = sysAdminService;
    }

    @Override
    protected Void executeWithAction(CommandContext context, VmActionRequest vmActionRequest) throws Exception {
        long hfsVmId = vmActionRequest.virtualMachine.hfsVmId;
        logger.info("Opening winexe ports for HFS VM ID {} ", hfsVmId);

        SysAdminAction hfsSysAction = context.execute("EnableWinexe",
                                                      ctx -> sysAdminService.enableWinexe(hfsVmId),
                                                      SysAdminAction.class);
        context.execute("WaitForEnableWinexe-" + hfsVmId, WaitForSysAdminAction.class, hfsSysAction);

        return null;
    }
}
