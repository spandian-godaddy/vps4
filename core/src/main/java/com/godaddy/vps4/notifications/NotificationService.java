package com.godaddy.vps4.notifications;

import com.godaddy.vps4.util.NotificationListSearchFilters;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public interface NotificationService {
    Notification getNotification(UUID notificationId);
    List<Notification> getNotifications(NotificationListSearchFilters searchFilters);
    Notification createNotification(UUID notificationId, NotificationType type, boolean supportOnly, boolean dismissible,
                            NotificationExtendedDetails notificationExtendedDetails, List<NotificationFilter> filters,
                                    Instant validOn, Instant validUntil);
    Notification updateNotification(UUID notificationId, NotificationType type, boolean supportOnly, boolean dismissible,
                                    NotificationExtendedDetails notificationExtendedDetails, List<NotificationFilter> filters,
                                    Instant validOn, Instant validUntil);
    void addFilterToNotification(UUID notificationId, List<NotificationFilter> filters);
    void deleteNotification(UUID notificationId);
    List<NotificationFilterType> getFilters();
}
