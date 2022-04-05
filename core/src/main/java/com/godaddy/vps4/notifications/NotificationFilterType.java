package com.godaddy.vps4.notifications;

import java.util.Map;

import static java.util.Arrays.stream;
import static java.util.stream.Collectors.toMap;

public enum NotificationFilterType {
    IMAGE_ID(1),
    RESELLER_ID(2),
    HYPERVISOR_HOSTNAME(3),
    TIER(4),
    PLATFORM_ID(5),
    VM_ID(6),
    IS_MANAGED(7),
    IS_IMPORTED(8);

    private final int typeId;

    NotificationFilterType(int typeId) {
        this.typeId = typeId;
    }

    private final static Map<Integer, NotificationFilterType> map = stream(NotificationFilterType.values())
            .collect(toMap(type -> type.typeId, type -> type));

    public static NotificationFilterType valueOf(int typeId) {
        return map.get(typeId);
    }

    public int getFilterTypeId() {
        return typeId;
    }
}