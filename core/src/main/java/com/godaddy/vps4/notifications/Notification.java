package com.godaddy.vps4.notifications;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public class Notification {
    public UUID notificationId;
    public NotificationType type;
    public boolean supportOnly;
    public boolean dismissible;
    public Instant validOn;
    public Instant validUntil;
    public List<NotificationFilter> filters;
    public NotificationExtendedDetails notificationExtendedDetails;
}