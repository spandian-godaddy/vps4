package com.godaddy.vps4.vm;

import java.util.UUID;

import org.apache.commons.lang3.builder.ToStringBuilder;

public class RestoreVmInfo {

    public UUID vmId;
    public UUID snapshotId;
    public String sgid;
    public String hostname;
    public String rawFlavor;
    public String username;
    public byte[] encryptedPassword;
    public String zone;
	public UUID orionGuid;

    public RestoreVmInfo() {
    }

    public RestoreVmInfo(UUID vmId, UUID snapshotId, String sgid, String hostname,
            String rawFlavor, String username, byte[] encryptedPassword, String zone, UUID orionGuid) {
        this.vmId = vmId;
        this.snapshotId = snapshotId;
        this.sgid = sgid;
        this.hostname = hostname;
        this.rawFlavor = rawFlavor;
        this.username = username;
        this.encryptedPassword = encryptedPassword;
        this.zone = zone;
        this.orionGuid = orionGuid;
    }

    public String toString() {
        return ToStringBuilder.reflectionToString(this);
    }
}
