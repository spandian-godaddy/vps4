package com.godaddy.vps4.vm;

import java.util.UUID;

import org.apache.commons.lang3.builder.ToStringBuilder;

public class ProvisionVmInfo {
    public UUID orionGuid;
    public String name;
    public String sgid;
    public long projectId;
    public int specId;
    public int managedLevel;
    public Image image;

    public ProvisionVmInfo() {        
    }
    
    public ProvisionVmInfo(UUID orionGuid, String name, long projectId, int specId, int managedLevel, Image image, String sgid) {
        this.orionGuid = orionGuid;
        this.name = name;
        this.projectId = projectId;
        this.sgid = sgid;
        this.specId = specId;
        this.managedLevel = managedLevel;
        this.image = image;
    }

    public String toString() {
        return ToStringBuilder.reflectionToString(this);
    }
}