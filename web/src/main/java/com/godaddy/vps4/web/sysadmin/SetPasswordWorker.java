package com.godaddy.vps4.web.sysadmin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.godaddy.vps4.vm.ActionStatus;
import com.godaddy.vps4.Vps4Exception;
import com.godaddy.vps4.web.Action;

import gdg.hfs.vhfs.sysadmin.SysAdminAction;
import gdg.hfs.vhfs.sysadmin.SysAdminService;

public class SetPasswordWorker implements Runnable{
    protected static final Logger logger = LoggerFactory.getLogger(SetPasswordWorker.class);

    final SysAdminService sysAdminService;
    final long vmId;
    final String[] usernames;
    final String password;
    SetPasswordAction action;
    
    public SetPasswordWorker(SysAdminService sysAdminService, SetPasswordAction action) {
        this.sysAdminService = sysAdminService;
        this.vmId = action.getVmId();
        this.usernames = action.getUsername();
        this.password = action.getPassword();
        this.action = action;
    }

    @Override
    public void run(){
        try {
            logger.debug("Setting passwords for users {} on vm {}", usernames.toString(), vmId);
            for(String username : usernames){
                logger.debug("BEGIN: Setting password for user {} on vm {}", username, vmId);
                SysAdminAction hfsSysAction = sysAdminService.changePassword(vmId, username, password);
                SysAdminWorker.waitForSysAdminAction(sysAdminService, hfsSysAction);
                logger.debug("END: Setting password for user {} on vm {}", username, vmId);
            }
            action.status = ActionStatus.COMPLETE;
        } catch (Exception e) {
            String message = String.format("Failed to change password for user {} on vm {}", usernames, vmId);
            action.status = ActionStatus.ERROR;
            logger.error(message, e);
            throw new Vps4Exception("CHANGE_PASSWORD_FAILED", message);
        }
    }
}
