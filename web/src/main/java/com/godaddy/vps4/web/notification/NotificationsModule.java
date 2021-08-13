package com.godaddy.vps4.web.notification;

import com.godaddy.vps4.notifications.NotificationService;
import com.godaddy.vps4.notifications.jdbc.JdbcNotificationService;
import com.google.inject.AbstractModule;

public class NotificationsModule extends AbstractModule {
    @Override
    protected void configure() {
        bind(NotificationService.class).to(JdbcNotificationService.class);
    }
}