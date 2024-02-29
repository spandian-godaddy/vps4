package com.godaddy.hfs.plesk;

public class PleskImageConfigRequest {
    public long serverId;
    public String pleskLicenseType;
    public String pleskPass;
    public String pleskUser;

    public PleskImageConfigRequest() {
    }

    public PleskImageConfigRequest(long serverId, String pleskUser, String pleskPass, String pleskLicenseType) {
        this.serverId = serverId;
        this.pleskLicenseType = pleskLicenseType;
        this.pleskPass = pleskPass;
        this.pleskUser = pleskUser;
    }
}
