package com.godaddy.vps4.web.sysadmin;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import gdg.hfs.vhfs.sysadmin.SysAdminAction;
import gdg.hfs.vhfs.sysadmin.SysAdminService;

public class SysAdminWorker {

    protected static final Logger logger = LoggerFactory.getLogger(SysAdminWorker.class);

    public static class ActionNotCompleteException extends RuntimeException {

        private static final long serialVersionUID = 1L;

        final SysAdminAction action;

        public ActionNotCompleteException(SysAdminAction action) {
            this.action = action;
        }

        public SysAdminAction getAction() {
            return action;
        }
    }

    public static void waitForSysAdminAction(SysAdminService sysAdminService, SysAdminAction sysAction) {

        while (sysAction.status.equals(SysAdminAction.Status.NEW) || sysAction.status.equals(SysAdminAction.Status.IN_PROGRESS)) {
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
            throw new ActionNotCompleteException(sysAction);
        }
    }
}
