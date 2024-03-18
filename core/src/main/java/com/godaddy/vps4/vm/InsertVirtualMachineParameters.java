package com.godaddy.vps4.vm;

import java.util.UUID;

public class InsertVirtualMachineParameters {
    public InsertVirtualMachineParameters(long hfsVmId, UUID orionGuid, String name, long projectId, int specId, long imageId, int dataCenterId, String hostname, String currentOs) {
        this.hfsVmId = hfsVmId;
        this.orionGuid = orionGuid;
        this.name = name;
        this.projectId = projectId;
        this.specId = specId;
        this.imageId = imageId;
        this.dataCenterId = dataCenterId;
        this.hostname = hostname;
        this.currentOs = currentOs;
    }

    public long hfsVmId;
    public UUID orionGuid;
    public String name;
    public long projectId;
    public int specId;
    public long imageId;
    public int dataCenterId;
    public String hostname;
    public String currentOs;
}
