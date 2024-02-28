package com.godaddy.vps4.orchestration.hfs.sysadmin;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import gdg.hfs.orchestration.Command;
import gdg.hfs.orchestration.CommandContext;
import com.godaddy.hfs.sysadmin.SysAdminService;
import com.godaddy.hfs.sysadmin.SysAdminAction;

public class UninstallPanoptaAgent implements Command<Long, Void> {

    private static final Logger logger = LoggerFactory.getLogger(UninstallPanoptaAgent.class);

    private final SysAdminService sysAdminService;

    @Inject
    public UninstallPanoptaAgent(SysAdminService sysAdminService) {
        this.sysAdminService = sysAdminService;
    }

    @Override
    public Void execute(CommandContext context, Long hfsVmId) {
        logger.info("Uninstalling panopta agent for hfs vm id {} ", hfsVmId);

        SysAdminAction hfsSysAction = context.execute("UninstallPanoptaAgent",
                                                      ctx -> sysAdminService.deletePanopta(hfsVmId),
                                                      SysAdminAction.class);
        context.execute("WaitForPanoptaUninstall-" + hfsVmId, WaitForSysAdminAction.class, hfsSysAction);

        return null;
    }
}
