package com.godaddy.vps4.vm;

import java.time.Instant;
import java.util.Set;
import java.util.UUID;

import org.apache.commons.lang3.builder.ReflectionToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

public class VmOutage {
    public UUID vmId;
    public Set<VmMetric> metrics;
    public Instant started;
    public Instant ended;
    public String reason;
    public long panoptaOutageId;

    @Override
    public String toString() {
        return ReflectionToStringBuilder.toString(this, ToStringStyle.MULTI_LINE_STYLE);
    }
}
