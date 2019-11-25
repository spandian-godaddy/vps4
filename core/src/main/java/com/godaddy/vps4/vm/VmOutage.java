package com.godaddy.vps4.vm;

import java.time.Instant;

public class VmOutage {
    public int outageId;
    public VmMetric metric;
    public Instant started;
    public Instant ended;
    public String reason;
    public long outageDetailId;
}
