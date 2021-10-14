package com.godaddy.vps4.notifications;

import java.util.Map;

import static java.util.Arrays.stream;
import static java.util.stream.Collectors.toMap;

public enum NotificationType {
    PATCHING(1),
    MAINTENANCE(2),
    NEW_MESSAGE_CENTER(3),
    NEW_FEATURE(4),
    GENERIC_OUTAGE(5);

    private final int typeId;

    NotificationType(int typeId) {
        this.typeId = typeId;
    }

    private final static Map<Integer, NotificationType> map = stream(NotificationType.values())
            .collect(toMap(type -> type.typeId, type -> type));

    public static NotificationType valueOf(int typeId) {
        return map.get(typeId);
    }

    public int getNotificationTypeId() {
        return typeId;
    }
}