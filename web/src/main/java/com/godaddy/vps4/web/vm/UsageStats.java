package com.godaddy.vps4.web.vm;

import java.time.Instant;

import org.apache.commons.lang3.builder.ReflectionToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;


public class UsageStats {
    public UsageStats.DiskUsage disk;
    public UsageStats.CpuUsage cpu;
    public UsageStats.MemUsage mem;

    public static class DiskUsage {
        public long diskUsed;
        public long diskTotal;
    }

    public static class CpuUsage {
        public double cpuUsagePercent;
    }

    public static class MemUsage {
        public long memUsed;
        public long memTotal;
    }

    public Instant lastRefreshedAt;
    public long utilizationId;

    @Override
    public String toString() {
        return ReflectionToStringBuilder.toString(this, ToStringStyle.MULTI_LINE_STYLE);
    }
}
