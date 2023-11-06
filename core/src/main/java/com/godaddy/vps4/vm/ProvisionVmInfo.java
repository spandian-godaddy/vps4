package com.godaddy.vps4.vm;

import java.util.UUID;

import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

public class ProvisionVmInfo {
    public UUID vmId;
    public String sgid;
    public boolean isManaged;
    public boolean hasMonitoring;
    public Image image;
    public int mailRelayQuota;
    public int diskGib;
    public boolean isPanoptaEnabled;
    public int previousRelays;
    public PleskLicenseType pleskLicenseType;

    public ProvisionVmInfo() {
    }

    public ProvisionVmInfo(UUID vmId, boolean isManaged, boolean hasMonitoring, Image image,
            String sgid, int mailRelayQuota, int diskGib, int previousRelays) {
        this.vmId = vmId;
        this.sgid = sgid;
        this.isManaged = isManaged;
        this.hasMonitoring = hasMonitoring;
        this.image = image;
        this.mailRelayQuota = mailRelayQuota;
        this.diskGib = diskGib;
        this.previousRelays = previousRelays;
    }

    @Override
    public String toString() {
        return ToStringBuilder.reflectionToString(this, ToStringStyle.MULTI_LINE_STYLE);
    }
}
