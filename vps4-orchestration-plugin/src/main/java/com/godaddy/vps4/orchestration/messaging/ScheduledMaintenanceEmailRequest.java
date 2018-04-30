package com.godaddy.vps4.orchestration.messaging;

import java.time.Instant;


public class ScheduledMaintenanceEmailRequest {
    public String shopperId;
    public String accountName;
    public Instant startTime;
    public long durationMinutes;
    public boolean isFullyManaged;

    public ScheduledMaintenanceEmailRequest(String shopperId, String accountName, boolean isFullyManaged,
                                            Instant startTime, long durationMinutes) {
        this.shopperId = shopperId;
        this.accountName = accountName;
        this.isFullyManaged = isFullyManaged;
        this.startTime = startTime;
        this.durationMinutes = durationMinutes;
    }
}
