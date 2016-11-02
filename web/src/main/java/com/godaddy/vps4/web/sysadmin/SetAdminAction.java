package com.godaddy.vps4.web.sysadmin;

import com.godaddy.vps4.web.Action;

public class SetAdminAction extends Action {

    private final String username;
    private final long vmId;
    private final boolean adminEnabled;

    public SetAdminAction(String username, long vmId, boolean adminEnabled) {
        this.username = username;
        this.vmId = vmId;
        this.adminEnabled = adminEnabled;
    }

    public String getUsername() {
        return username;
    }

    public long getVmId() {
        return vmId;
    }
    
    public boolean getAdminEnabled(){
        return adminEnabled;
    }

}
