package com.godaddy.hfs.vm;

import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

import com.fasterxml.jackson.annotation.JsonProperty;

public class HfsInventoryData {

    String name;
    int available;
    @JsonProperty("in_use")
    int inUse;
    @JsonProperty("hfs_in_use")
    int hfsInUse;
    int reserved;
    int retired;

    public String getName() {
        return name;
    }

    public int getAvailable() {
        return available;
    }

    public int getInUse() { return inUse; }

    public int getHfsInUse() { return hfsInUse; }

    public int getReserved() {
        return reserved;
    }

    public int getRetired() { return retired; }

    @Override
    public String toString() {
        return ToStringBuilder.reflectionToString(this, ToStringStyle.SHORT_PREFIX_STYLE);
    }
}
