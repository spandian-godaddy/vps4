package com.godaddy.vps4.web.sysadmin;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.godaddy.vps4.Vps4Exception;

import gdg.hfs.vhfs.sysadmin.SysAdminAction;
import gdg.hfs.vhfs.sysadmin.SysAdminService;

public abstract class SysAdminWorker implements Runnable {

    protected static final Logger logger = LoggerFactory.getLogger(SysAdminWorker.class);

    SysAdminService sysAdminService;
    
    public SysAdminWorker(SysAdminService sysAdminService) {
        this.sysAdminService = sysAdminService;
    }

    @Override
    public abstract void run();
    protected abstract Vps4Exception getException();
    
    public void waitForSysAdminAction(SysAdminAction sysAction) {
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
            throw getException();
        }
    }
}
