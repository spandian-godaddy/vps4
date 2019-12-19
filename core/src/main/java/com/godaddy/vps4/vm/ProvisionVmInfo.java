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

    public ProvisionVmInfo() {
    }

    public ProvisionVmInfo(UUID vmId, boolean isManaged, boolean hasMonitoring, Image image,
            String sgid, int mailRelayQuota, int diskGib) {
        this.vmId = vmId;
        this.sgid = sgid;
        this.isManaged = isManaged;
        this.hasMonitoring = hasMonitoring;
        this.image = image;
        this.mailRelayQuota = mailRelayQuota;
        this.diskGib = diskGib;
    }

    @Override
    public String toString() {
        return ToStringBuilder.reflectionToString(this, ToStringStyle.MULTI_LINE_STYLE);
    }
}
