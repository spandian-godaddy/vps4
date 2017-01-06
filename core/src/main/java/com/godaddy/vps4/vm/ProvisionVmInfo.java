package com.godaddy.vps4.vm;

import java.util.UUID;

import org.apache.commons.lang3.builder.ToStringBuilder;

public class ProvisionVmInfo {
    public UUID vmId;
    public String sgid;
    public int managedLevel;
    public Image image;

    public ProvisionVmInfo() {        
    }
    
    public ProvisionVmInfo(UUID vmId, int managedLevel, Image image, String sgid) {
        this.vmId = vmId;
        this.sgid = sgid;
        this.managedLevel = managedLevel;
        this.image = image;
    }

    public String toString() {
        return ToStringBuilder.reflectionToString(this);
    }
}