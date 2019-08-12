package com.godaddy.vps4.vm;

import java.time.Instant;

import org.apache.commons.lang3.builder.ReflectionToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

public class ServerSpec {

    public int specId;
    public String name;
    public String specName;
    public int tier;
    public int cpuCoreCount;
    public int memoryMib;
    public int diskGib;
    public Instant validOn;
    public Instant validUntil;
    public ServerType serverType;

    public ServerSpec() {
    }

    public ServerSpec(int specId, String name, String specName, int tier, int cpuCoreCount, int memoryMib, int diskGib,
                      Instant validOn, Instant validUntil, ServerType serverType) {
        this.specId = specId;
        this.name = name;
        this.specName = specName;
        this.tier = tier;
        this.cpuCoreCount = cpuCoreCount;
        this.memoryMib = memoryMib;
        this.diskGib = diskGib;
        this.validOn = validOn;
        this.validUntil = validUntil;
        this.serverType = serverType;
    }

    public boolean isVirtualMachine() {
        return serverType.serverType == ServerType.Type.VIRTUAL;
    }

    @Override
    public String toString() {
        return ReflectionToStringBuilder.toString(this, ToStringStyle.MULTI_LINE_STYLE);
    }

}
