package com.godaddy.vps4.vm;

import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

import java.util.UUID;

public class ProvisionVmInfo {
    public UUID vmId;
    public String sgid;
    public int managedLevel;
    public boolean hasMonitoring;
    public Image image;
    public int mailRelayQuota;
    public int diskGib;
    public static final int FULLY_MANAGED_LEVEL = 2;

    public ProvisionVmInfo() {
    }

    public ProvisionVmInfo(UUID vmId, int managedLevel, boolean hasMonitoring, Image image,
            String sgid, int mailRelayQuota, int diskGib) {
        this.vmId = vmId;
        this.sgid = sgid;
        this.managedLevel = managedLevel;
        this.hasMonitoring = hasMonitoring;
        this.image = image;
        this.mailRelayQuota = mailRelayQuota;
        this.diskGib = diskGib;
    }

    @Override
    public String toString() {
        return ToStringBuilder.reflectionToString(this, ToStringStyle.MULTI_LINE_STYLE);
    }

    public boolean isFullyManaged() {
        return this.managedLevel == FULLY_MANAGED_LEVEL;
    }
}