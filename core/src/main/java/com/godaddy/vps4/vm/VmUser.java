package com.godaddy.vps4.vm;

import java.util.UUID;

public class VmUser {
    
    public final UUID vmId;
    public final String username;
    public final boolean adminEnabled;
    

    public VmUser(String username, UUID vmId, boolean adminEnabled) {
        this.vmId = vmId;
        this.username = username;
        this.adminEnabled = adminEnabled;
    }

    @Override
    public String toString() {
        return "User [\"adminEnabled = " + adminEnabled 
                   + " username = " + username + " vmId=" + vmId + " ]";
    }

}
