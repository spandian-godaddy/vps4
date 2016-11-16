package com.godaddy.vps4.vm;

public class User {
    
    public final long vmId;
    public final String username;
    public final boolean adminEnabled;
    

    public User(String username, long vmId, boolean adminEnabled) {
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
