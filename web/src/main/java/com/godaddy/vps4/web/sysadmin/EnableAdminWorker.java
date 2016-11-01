package com.godaddy.vps4.web.sysadmin;
import com.godaddy.vps4.Vps4Exception;

import gdg.hfs.vhfs.sysadmin.SysAdminAction;
import gdg.hfs.vhfs.sysadmin.SysAdminService;

public class EnableAdminWorker extends SysAdminWorker {

    final long vmId;
    final String username;
    
    public EnableAdminWorker(SysAdminService sysAdminService, long vmId, String username) {
        super(sysAdminService);
        this.sysAdminService = sysAdminService;
        this.vmId = vmId;
        this.username = username;
    }

    @Override
    public void run(){
        logger.debug("Enabling admin access for vm {}", vmId);
        SysAdminAction sysAction = sysAdminService.enableAdmin(vmId, username);
        waitForSysAdminAction(sysAction);
    }
    
    protected Vps4Exception getException(){
        return new Vps4Exception("ENABLE_ADMIN_WORKER_FAILED",
               String.format("Failed to enable admin for vm: %d", vmId));
    }

}
