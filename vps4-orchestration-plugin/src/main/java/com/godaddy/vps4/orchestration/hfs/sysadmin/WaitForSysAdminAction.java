package com.godaddy.vps4.orchestration.hfs.sysadmin;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.godaddy.vps4.orchestration.hfs.ActionNotCompletedException;

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

        while (sysAction.status == SysAdminAction.Status.NEW
                || sysAction.status == SysAdminAction.Status.IN_PROGRESS) {
            logger.info("waiting on System Admin Action: {}", sysAction);
            try {
                Thread.sleep(2000);
            }
            catch (InterruptedException e) {
                logger.warn("Interrupted while sleeping");
            }
            sysAction = sysAdminService.getSysAdminAction(sysAction.sysAdminActionId);
        }

        if (!sysAction.status.equals(SysAdminAction.Status.COMPLETE)) {
            throw new ActionNotCompletedException(sysAction);
        }

        return sysAction;
    }
}
