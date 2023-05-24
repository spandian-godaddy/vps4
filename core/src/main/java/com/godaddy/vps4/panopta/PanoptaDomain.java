package com.godaddy.vps4.panopta;

import java.time.Instant;

import com.godaddy.vps4.vm.VmMetric;

public class PanoptaDomain extends PanoptaMetricId {
    public Instant validOn;
    public VmMetric vmMetric;

    public PanoptaDomain(PanoptaMetricId metricId, VmMetric metric, Instant validOn) {
        this.id = metricId.id;
        this.typeId = metricId.typeId;
        this.serverInterface = metricId.serverInterface;
        this.status = metricId.status;
        this.metadata = metricId.metadata;
        this.vmMetric = metric;
        this.validOn = validOn;
    }
}
