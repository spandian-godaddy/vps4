package com.godaddy.vps4.vm;

import java.time.Instant;

import org.apache.commons.lang3.builder.ReflectionToStringBuilder;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

public class VmOutage {
    public int outageId;
    public VmMetric metric;
    public Instant started;
    public Instant ended;
    public String reason;
    public long outageDetailId;

    public int getOutageId() {
        return outageId;
    }

    public VmMetric getMetric() {
        return metric;
    }

    public Instant getStarted() {
        return started;
    }

    public Instant getEnded() {
        return ended;
    }

    public String getReason() {
        return reason;
    }

    public long getOutageDetailId() {
        return outageDetailId;
    }

    public String toString() {
        return ReflectionToStringBuilder.toString(this, ToStringStyle.MULTI_LINE_STYLE);
    }
}
