package com.godaddy.vps4.web.sysadmin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.godaddy.vps4.Vps4Exception;

import gdg.hfs.vhfs.sysadmin.SysAdminAction;
import gdg.hfs.vhfs.sysadmin.SysAdminService;


public class ToggleAdminWorker implements Runnable{
    protected static final Logger logger = LoggerFactory.getLogger(ToggleAdminWorker.class);


    final SysAdminService sysAdminService;
    final long vmId;
    final String username;
    final boolean enabled;
    
    public ToggleAdminWorker(SysAdminService sysAdminService, long vmId, String username, boolean enabled) {
        this.sysAdminService = sysAdminService;
        this.vmId = vmId;
        this.username = username;
        this.enabled = enabled;
    }

    @Override
    public void run(){
        try {
            // Paul Tanganelli [10/28/16] (HFS) Looks like calling disableAdmin before enableAdmin causes problems.
            logger.debug("Setting admin access to {} for user {} in vm {}", enabled, username, vmId);

            SysAdminAction sysAction = sysAdminService.enableAdmin(vmId, username);
            SysAdminWorker.waitForSysAdminAction(sysAdminService, sysAction);

            if (!enabled) {
                sysAction = sysAdminService.disableAdmin(vmId, username);
                SysAdminWorker.waitForSysAdminAction(sysAdminService, sysAction);
            }
        } catch (Exception e) {
            String toggle = enabled ? "ENABLE" : "DISABLE";
            String message = String.format("Failed to %s admin for vm: %d", toggle, vmId);
            logger.error(message, e);
            throw new Vps4Exception(toggle+"_ADMIN_WORKER_FAILED", message);
        }
    }
}
