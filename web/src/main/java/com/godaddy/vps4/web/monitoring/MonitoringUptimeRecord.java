package com.godaddy.vps4.web.monitoring;

public class MonitoringUptimeRecord {
    public MonitoringUptimeRecord(String label, double uptime) {
        this.label = label;
        this.uptime = uptime;
    }

    public String label;
    public double uptime;
}
