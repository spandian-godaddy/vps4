package com.godaddy.vps4.vm;

import java.util.UUID;

import org.apache.commons.lang3.builder.ToStringBuilder;

import com.godaddy.vps4.network.IpAddress;

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
    public String serverName;
    public String privateLabelId;
    public UUID orionGuid;
    public String shopperId;

    public RebuildVmInfo() {
    }

    @Override
    public String toString() {
        return ToStringBuilder.reflectionToString(this);
    }
}