package com.godaddy.vps4.web.monitoring;

import gdg.hfs.vhfs.nodeping.NodePingUptimeRecord;

public class MonitoringUptimeRecord {
    public MonitoringUptimeRecord(NodePingUptimeRecord record) {

        this.label = record.label;
        uptime = record.uptime;
    }

    public String label;
    public double uptime;
}
