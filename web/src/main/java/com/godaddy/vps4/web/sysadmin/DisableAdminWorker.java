package com.godaddy.vps4.web.sysadmin;

import com.godaddy.vps4.Vps4Exception;

import gdg.hfs.vhfs.sysadmin.SysAdminAction;
import gdg.hfs.vhfs.sysadmin.SysAdminService;

public class DisableAdminWorker extends SysAdminWorker {

    final long vmId;
    final String username;
    
    public DisableAdminWorker(SysAdminService sysAdminService, long vmId, String username) {
        super(sysAdminService);
        this.sysAdminService = sysAdminService;
        this.vmId = vmId;
        this.username = username;
        
    }

    @Override
    public void run(){
     // Paul Tanganelli [10/28/16] (HFS) Looks like calling disableAdmin before enableAdmin causes problems.
        logger.debug("Disabling admin access for vm {}", vmId);
        SysAdminAction sysAction = sysAdminService.enableAdmin(vmId, username);
        waitForSysAdminAction(sysAction);
        sysAction = sysAdminService.disableAdmin(vmId, username);
        waitForSysAdminAction(sysAction);
    }
    
    protected Vps4Exception getException(){
        return new Vps4Exception("DISABLE_ADMIN_WORKER_FAILED",
                                String.format("Failed to disable admin for vm: %d", vmId));
    }
    
    

}
