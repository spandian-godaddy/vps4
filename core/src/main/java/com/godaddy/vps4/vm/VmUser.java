package com.godaddy.vps4.vm;

import java.util.UUID;

public class VmUser {
    
    public UUID vmId;
    public String username;
    public boolean adminEnabled;
    public VmUserType vmUserType;

    public VmUser () {}

    public VmUser(String username, UUID vmId, boolean adminEnabled, VmUserType vmUserType) {
        this.vmId = vmId;
        this.username = username;
        this.adminEnabled = adminEnabled;
        this.vmUserType = vmUserType;
    }

    @Override
    public String toString() {
        return "User [adminEnabled= " + adminEnabled
                + ", username= " + username
                + ", vmId=" + vmId
                + ", vmUserType=" + vmUserType + "]";
    }
}
