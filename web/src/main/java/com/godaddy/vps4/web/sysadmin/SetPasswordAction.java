package com.godaddy.vps4.web.sysadmin;

import com.godaddy.vps4.web.Action;

public class SetPasswordAction extends Action {

    private final long vmId;
    private final String[] usernames;
    private final String password;

    public SetPasswordAction(long vmId, String username, String password) {
        this.usernames = new String[] {username};
        this.vmId = vmId;
        this.password = password;
    }
    
    public SetPasswordAction(long vmId, String[] usernames, String password) {
        this.usernames = usernames;
        this.vmId = vmId;
        this.password = password;
    }

    public String[] getUsername() {
        return usernames;
    }
    
    public String getPassword() {
        return password;
    }

    public long getVmId() {
        return vmId;
    }
}
