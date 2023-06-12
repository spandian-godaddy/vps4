package com.godaddy.vps4.web.monitoring;

import java.time.Instant;

import com.godaddy.vps4.vm.VmOutage;

public class MonitoringEvent {
    public String type;
    public Instant start;
    public Instant end;
    public boolean open;
    public String message;

    // Empty constructor required for Jackson
    public MonitoringEvent() {}

    public MonitoringEvent(VmOutage sourceEvent) {
        this.type = "outage";
        this.start = sourceEvent.started;
        this.end = sourceEvent.ended;
        this.open = sourceEvent.ended == null;
        this.message = sourceEvent.reason;
    }
}
