package com.godaddy.vps4.web.messaging;

public class ScheduledMessagingResourceRequest {
    public ScheduledMessagingResourceRequest() {

    }

    public ScheduledMessagingResourceRequest(String startTime, long durationMinutes) {
        this.startTime = startTime;
        this.durationMinutes = durationMinutes;
    }

    public String startTime;
    public long durationMinutes;
}
