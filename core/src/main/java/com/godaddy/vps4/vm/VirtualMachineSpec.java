package com.godaddy.vps4.vm;

import java.time.Instant;

public class VirtualMachineSpec {

    public final int specId;

    public final String name;
    public final int tier;

    public final int cpuCoreCount;
    public final int memoryMib;
    public final int diskGib;

    public final Instant validOn;
    public final Instant validUntil;

    public VirtualMachineSpec(int specId, String name, int tier, int cpuCoreCount, int memoryMib,
            int diskGib, Instant validOn, Instant validUntil) {
        this.specId = specId;
        this.name = name;
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
