package com.godaddy.vps4.vm;

import java.util.UUID;

public class ProvisionVmInfo {
    public UUID orionGuid;
    public String name;
    public long projectId;
    public int specId;
    public int managedLevel;
    public Image image;

    public ProvisionVmInfo(UUID orionGuid, String name, long projectId, int specId,
            int managedLevel, Image image) {
        this.orionGuid = orionGuid;
        this.name = name;
        this.projectId = projectId;
        this.specId = specId;
        this.managedLevel = managedLevel;
        this.image = image;
    }
}