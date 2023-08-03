package com.godaddy.vps4.vm;

import java.util.UUID;

public class ProvisionVirtualMachineParameters {
    public ProvisionVirtualMachineParameters(long vps4UserId,
                                             int dataCenterId,
                                             String sgidPrefix,
                                             UUID orionGuid,
                                             String name,
                                             int tier,
                                             int managedLevel,
                                             String image) {
        this.vps4UserId = vps4UserId;
        this.dataCenterId = dataCenterId;
        this.sgidPrefix = sgidPrefix;
        this.orionGuid = orionGuid;
        this.name = name;
        this.tier = tier;
        this.managedLevel = managedLevel;
        this.imageHfsName = image;
    }

    private long vps4UserId;
    private int dataCenterId;
    private String sgidPrefix;
    private UUID orionGuid;
    private String name;
    private int tier;
    private int managedLevel;
    private String imageHfsName;

    public long getVps4UserId() {
        return vps4UserId;
    }

    public int getDataCenterId() {
        return dataCenterId;
    }

    public String getSgidPrefix() {
        return sgidPrefix;
    }

    public UUID getOrionGuid() {
        return orionGuid;
    }

    public String getName() {
        return name;
    }

    public int getTier() {
        return tier;
    }

    public int getManagedLevel() {
        return managedLevel;
    }

    public String getImageHfsName() {
        return imageHfsName;
    }
}
