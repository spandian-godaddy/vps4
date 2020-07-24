package com.godaddy.vps4.vm;

import java.time.Instant;
import java.util.UUID;

import org.apache.commons.lang3.builder.ReflectionToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

public class VmOutage {
    public UUID vmId;
    public int outageId;
    public VmMetric metric;
    public Instant started;
    public Instant ended;
    public String reason;
    public long outageDetailId;

    @Override
    public String toString() {
        return ReflectionToStringBuilder.toString(this, ToStringStyle.MULTI_LINE_STYLE);
    }
}
