package com.godaddy.vps4.vm;

import java.time.Instant;

public class VirtualMachineSpec {

    public int specId;

    public String name;
    public String specName;
    public int tier;

    public int cpuCoreCount;
    public int memoryMib;
    public int diskGib;

    public Instant validOn;
    public Instant validUntil;
    
    public VirtualMachineSpec() {
    }

    public VirtualMachineSpec(int specId, String name, String specName, int tier, int cpuCoreCount, int memoryMib,
            int diskGib, Instant validOn, Instant validUntil) {
        this.specId = specId;
        this.name = name;
        this.specName = specName;
        this.tier = tier;
        this.cpuCoreCount = cpuCoreCount;
        this.memoryMib = memoryMib;
        this.diskGib = diskGib;
        this.validOn = validOn;
        this.validUntil = validUntil;
        
    }

    @Override
    public String toString() {
        return "VirtualMachineSpec [specId=" + specId + ", cpuCoreCount="
                + cpuCoreCount + ", memoryMib=" + memoryMib + ", diskGib="
                + diskGib + ", validOn=" + validOn + ", validUntil="
                + validUntil + "]";
    }

}
