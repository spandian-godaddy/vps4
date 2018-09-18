package com.godaddy.vps4.vm;

import static java.util.Arrays.stream;
import static java.util.stream.Collectors.toMap;

import java.util.Map;

public enum ActionType {

    CREATE_VM(1),
    RESTART_VM(2),
    ENABLE_ADMIN_ACCESS(3),
    DISABLE_ADMIN_ACCESS(4),
    START_VM(5),
    STOP_VM(6),
    DESTROY_VM(7),
    SET_PASSWORD(8),
    SET_HOSTNAME(9),
    UPDATE_SERVER(10),
    ADD_SUPPORT_USER(11),
    REMOVE_SUPPORT_USER(12),
    CREATE_SNAPSHOT(13),
    DESTROY_SNAPSHOT(14),
    PUBLISH_SNAPSHOT(15),
    ADD_IP(16),
    DESTROY_IP(17),
    UPDATE_MAILRELAY_QUOTA(18),
    RENAME_SNAPSHOT(19),
    RESTORE_VM(20),
    CANCEL_ACCOUNT(21),
    RESTORE_ACCOUNT(22),
    UPGRADE_VM(23),
    REBUILD_VM(24);


    private final int typeId;

    ActionType(int typeId) {
        this.typeId = typeId;
    }

    private final static Map<Integer, ActionType> map = stream(ActionType.values())
            .collect(toMap(type -> type.typeId, type -> type));

    public static ActionType valueOf(int typeId) {
        return map.get(typeId);
    }

    public int getActionTypeId() {
        return typeId;
    }

}
