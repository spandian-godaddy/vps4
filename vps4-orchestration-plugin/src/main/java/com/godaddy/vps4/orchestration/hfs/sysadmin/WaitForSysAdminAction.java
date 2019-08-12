package com.godaddy.vps4.orchestration.hfs.sysadmin;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.godaddy.vps4.orchestration.hfs.SysAdminActionNotCompletedException;
import com.godaddy.vps4.orchestration.scheduler.Utils;

import gdg.hfs.orchestration.Command;
import gdg.hfs.orchestration.CommandContext;
import gdg.hfs.vhfs.sysadmin.SysAdminAction;
import gdg.hfs.vhfs.sysadmin.SysAdminService;

public class WaitForSysAdminAction implements Command<SysAdminAction, SysAdminAction> {

    private static final Logger logger = LoggerFactory.getLogger(WaitForSysAdminAction.class);

    private final SysAdminService sysAdminService;

    @Inject
    public WaitForSysAdminAction(SysAdminService sysAdminService) {
        this.sysAdminService = sysAdminService;
    }

    @Override
    public SysAdminAction execute(CommandContext context, SysAdminAction sysAction) {

        long sysAdminActionId = sysAction.sysAdminActionId;
        while (sysAction.status == SysAdminAction.Status.NEW
                || sysAction.status == SysAdminAction.Status.IN_PROGRESS) {
            logger.debug("waiting on System Admin Action: {}", sysAction);
            sysAction = Utils.runWithRetriesForServerErrorException(context, logger, () ->{
                return sysAdminService.getSysAdminAction(sysAdminActionId);
            });
        }

        if(sysAction.status == SysAdminAction.Status.COMPLETE) {
            logger.info("System Admin Action completed. {} ", sysAction );
        } else {
            throw new SysAdminActionNotCompletedException(sysAction);
        }

        return sysAction;
    }
}
