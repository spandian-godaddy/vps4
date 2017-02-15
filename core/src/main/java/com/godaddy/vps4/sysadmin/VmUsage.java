package com.godaddy.vps4.sysadmin;

public class VmUsage {

    public DiskUsage disk;

    public IoUsage io;

    public CpuUsage cpu;

    public MemUsage mem;

    public static class DiskUsage {
        // TODO deviceId?
        public long mibUsed;
        public long mibAvail;
    }

    public static class IoUsage {
        public double totalTps;
        public double readTps;
        public double writeTps;
    }

    public static class CpuUsage {
        public double userPercent;
        public double systemPercent;
    }

    public static class MemUsage {
        public long mibMemFree;
        public long mibMemUsed;
    }
}
