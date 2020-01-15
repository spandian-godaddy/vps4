package com.godaddy.vps4.vm;

public class VmMetricAlert {
    public enum Status {ENABLED, DISABLED}

    public VmMetric metric;
    public String type;
    public Status status;
}
