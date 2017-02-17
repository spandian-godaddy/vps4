package com.godaddy.vps4.sysadmin;

import java.time.Instant;

public class VmUsage {

    public DiskUsage disk;

    public IoUsage io;

    public CpuUsage cpu;

    public MemUsage mem;

    public static class DiskUsage {
        public Instant timestamp;
        // TODO deviceId?
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
}
