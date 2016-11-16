package com.godaddy.vps4.web.sysadmin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.godaddy.vps4.vm.ActionStatus;
import com.godaddy.vps4.Vps4Exception;
import com.godaddy.vps4.vm.UserService;
import com.godaddy.vps4.web.Action;


import gdg.hfs.vhfs.sysadmin.SysAdminAction;
import gdg.hfs.vhfs.sysadmin.SysAdminService;


public class ToggleAdminWorker implements Runnable{
    protected static final Logger logger = LoggerFactory.getLogger(ToggleAdminWorker.class);


    final SysAdminService sysAdminService;
    final UserService userService;
    final long vmId;
    final String username;
    final boolean enabled;
    SetAdminAction action;
    
    public ToggleAdminWorker(SysAdminService sysAdminService, UserService userService, SetAdminAction action) {
        this.sysAdminService = sysAdminService;
        this.userService = userService;
        this.vmId = action.getVmId();
        this.username = action.getUsername();
        this.enabled = action.getAdminEnabled();
        this.action = action;
    }
    
    public ToggleAdminWorker(SysAdminService sysAdminService, UserService userService, long vmId, String username, boolean enabled) {
        this.sysAdminService = sysAdminService;
        this.userService = userService;
        this.vmId = vmId;
        this.username = username;
        this.enabled = enabled;
        this.action = new SetAdminAction(username, vmId, enabled);
    }

    @Override
    public void run(){
        try {
            // Paul Tanganelli [10/28/16] (HFS) Looks like calling disableAdmin before enableAdmin causes problems.
            logger.debug("Setting admin access to {} for user {} in vm {}", enabled, username, vmId);

            SysAdminAction hfsSysAction = sysAdminService.enableAdmin(vmId, username);
            SysAdminWorker.waitForSysAdminAction(sysAdminService, hfsSysAction);

            if (!enabled) {
                hfsSysAction = sysAdminService.disableAdmin(vmId, username);
                SysAdminWorker.waitForSysAdminAction(sysAdminService, hfsSysAction);
            }
            
            userService.updateUserAdminAccess(action.getUsername(), action.getVmId(), enabled);

            action.status = ActionStatus.COMPLETE;
        } catch (Exception e) {
            String toggle = enabled ? "ENABLE" : "DISABLE";
            String message = String.format("Failed to %s admin for vm: %d", toggle, vmId);
            action.status = ActionStatus.ERROR;
            logger.error(message, e);
            throw new Vps4Exception(toggle+"_ADMIN_WORKER_FAILED", message);
        }
    }
}
