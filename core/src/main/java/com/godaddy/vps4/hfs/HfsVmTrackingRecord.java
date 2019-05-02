package com.godaddy.vps4.hfs;

import java.time.Instant;
import java.util.UUID;

import org.apache.commons.lang3.builder.ReflectionToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

public class HfsVmTrackingRecord {
    public long hfsVmId;
    public UUID vmId;
    public UUID orionGuid;
    public Instant requested;
    public Instant created;
    public Instant canceled;
    public Instant destroyed;

    @Override
    public String toString() {
        return ReflectionToStringBuilder.toString(this, ToStringStyle.SHORT_PREFIX_STYLE);
    }
}
