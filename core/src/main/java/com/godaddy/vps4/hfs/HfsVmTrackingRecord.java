package com.godaddy.vps4.hfs;

import java.time.Instant;
import java.util.UUID;

public class HfsVmTrackingRecord {
    public long hfsVmId;
    public UUID vmId;
    public UUID orionGuid;
    public Instant requested;
    public Instant created;
    public Instant canceled;
    public Instant destroyed;
}
