package com.godaddy.vps4.vm;

import java.util.UUID;

import com.godaddy.vps4.network.IpAddress;
import org.apache.commons.lang3.builder.ToStringBuilder;

public class RebuildVmInfo {

    public UUID vmId;
    public IpAddress ipAddress;
    public String sgid;
    public String hostname;
    public String rawFlavor;
    public Image image;
    public String username;
    public byte[] encryptedPassword;
    public String zone;

    public RebuildVmInfo() {
    }

    public RebuildVmInfo(UUID vmId, IpAddress ipAddress, String sgid, String hostname, Image image,
                         String rawFlavor, String username, byte[] encryptedPassword, String zone) {
        this.vmId = vmId;
        this.ipAddress = ipAddress;
        this.sgid = sgid;
        this.hostname = hostname;
        this.rawFlavor = rawFlavor;
        this.image = image;
        this.username = username;
        this.encryptedPassword = encryptedPassword;
        this.zone = zone;
    }

    public String toString() {
        return ToStringBuilder.reflectionToString(this);
    }
}