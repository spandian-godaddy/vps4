package com.godaddy.vps4.web.monitoring;

import java.time.Instant;

import gdg.hfs.vhfs.nodeping.NodePingEvent;

public class MonitoringEvent {
    public String type;
    public Instant start;
    public Instant end;
    public boolean open;
    public String message;

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
