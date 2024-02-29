package com.godaddy.hfs.plesk;

public class PleskAdminPassRequest {
    public long serverId;
    public String pleskAdminPass;

    public PleskAdminPassRequest() {
    }

    public PleskAdminPassRequest(long serverId, String pleskAdminPass) {
        this.serverId = serverId;
        this.pleskAdminPass = pleskAdminPass;
    }
}
