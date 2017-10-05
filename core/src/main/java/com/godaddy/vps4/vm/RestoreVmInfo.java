package com.godaddy.vps4.vm;

import org.apache.commons.lang3.builder.ToStringBuilder;

import java.util.UUID;

public class RestoreVmInfo {

    public UUID vmId;
    public UUID snapshotId;
    public String sgid;
    public String hostname;
    public String rawFlavor;
    public String username;
    public String password;
    public String zone;

    public RestoreVmInfo() {
    }

    public RestoreVmInfo(UUID vmId, UUID snapshotId, String sgid, String hostname,
                         String rawFlavor, String username, String password, String zone) {
        this.vmId = vmId;
        this.snapshotId = snapshotId;
        this.sgid = sgid;
        this.hostname = hostname;
        this.rawFlavor = rawFlavor;
        this.username = username;
        this.password = password;
        this.zone = zone;
    }

    public String toString() {
        return ToStringBuilder.reflectionToString(this);
    }
}