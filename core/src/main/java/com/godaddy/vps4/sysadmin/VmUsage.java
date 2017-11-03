package com.godaddy.vps4.sysadmin;

import java.time.Duration;
import java.time.Instant;

import org.json.simple.JSONObject;

import com.fasterxml.jackson.annotation.JsonProperty;

public class VmUsage {
    private static final Duration HFS_MIN_REFRESH_INTERVAL = Duration.ofMinutes(10);

    public DiskUsage disk;
    public IoUsage io;
    public CpuUsage cpu;
    public MemUsage mem;

    public static class DiskUsage {
        public Instant timestamp;
        public long mibUsed;
        public long mibAvail;
    }

    public static class IoUsage {
        public Instant timestamp;
        public double totalTps;
        public double readTps;
        public double writeTps;
    }

    public static class CpuUsage {
        public Instant timestamp;
        public double userPercent;
        public double systemPercent;
    }

    public static class MemUsage {
        public Instant timestamp;
        public long mibMemFree;
        public long mibMemUsed;
    }

    public Instant refreshedAt = Instant.now();
    public long pendingHfsActionId;

    @JsonProperty
    public Instant canRefreshAgainAt() {
        return refreshedAt.plus(HFS_MIN_REFRESH_INTERVAL);
    }

    public boolean canRefresh() {
        return !isRefreshInProgress() && Instant.now().isAfter(canRefreshAgainAt());
    }

    public boolean isRefreshInProgress() {
        return pendingHfsActionId > 0;
    }

    public void markRefreshCompleted(Instant refreshedAt) {
        this.refreshedAt = refreshedAt;
        pendingHfsActionId = 0;
    }

    public void updateUsageStats(JSONObject hfsUsageResults) {
        VmUsageParser parser = new VmUsageParser(hfsUsageResults);
        cpu = parser.parseCpu();
        disk = parser.parseDisk();
        mem = parser.parseMemory();
    }

    @Override
    public String toString() {
        return "VmUsage [disk=" + disk + ", io=" + io + ", cpu=" + cpu + ", mem=" + mem + ", refreshedAt=" + refreshedAt
                + ", pendingHfsActionId=" + pendingHfsActionId + ", canRefreshAgainAt()=" + canRefreshAgainAt()
                + ", isRefreshInProgress()=" + isRefreshInProgress() + "]";
    }

}
