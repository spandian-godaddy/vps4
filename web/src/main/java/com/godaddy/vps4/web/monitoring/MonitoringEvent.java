package com.godaddy.vps4.web.monitoring;

import java.time.Instant;

import com.godaddy.vps4.vm.VmOutage;

import gdg.hfs.vhfs.nodeping.NodePingEvent;

public class MonitoringEvent {
    public String type;
    public Instant start;
    public Instant end;
    public boolean open;
    public String message;

    public MonitoringEvent() {}

    public MonitoringEvent(VmOutage sourceEvent) {
        this.type = "outage";
        this.start = sourceEvent.started;
        this.end = sourceEvent.ended;
        this.open = sourceEvent.ended == null;
        this.message = sourceEvent.reason;
    }

    public MonitoringEvent(NodePingEvent sourceEvent) {
        this.type = sourceEvent.type;
        this.start = Instant.ofEpochMilli(sourceEvent.start);
        if (sourceEvent.end != null) {
            this.end = Instant.ofEpochMilli(sourceEvent.end);
        }
        this.open = sourceEvent.open;
        this.message = sourceEvent.message;
    }
}
