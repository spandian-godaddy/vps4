package com.godaddy.vps4.orchestration.messaging;

import java.time.Instant;
import java.util.UUID;


public class ScheduledMaintenanceEmailRequest {
    public UUID customerId;
    public String accountName;
    public Instant startTime;
    public long durationMinutes;
    public boolean isManaged;

    public ScheduledMaintenanceEmailRequest() {
    }
    public ScheduledMaintenanceEmailRequest(UUID customerId, String accountName, boolean isManaged,
                                            Instant startTime, long durationMinutes) {
        this.customerId = customerId;
        this.accountName = accountName;
        this.isManaged = isManaged;
        this.startTime = startTime;
        this.durationMinutes = durationMinutes;
    }
}
