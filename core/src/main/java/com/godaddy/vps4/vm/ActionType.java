package com.godaddy.vps4.vm;

import java.util.Map;

import static java.util.Arrays.stream;
import static java.util.stream.Collectors.toMap;

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
    REBUILD_VM(24),
    ABUSE_SUSPEND(25),
    REINSTATE(26),
    POWER_CYCLE(27),
    PAUSE_AUTO_SNAPSHOT(28),
    RESUME_AUTO_SNAPSHOT(29),
    SCHEDULE_MANUAL_SNAPSHOT(30),
    RESCHEDULE_AUTO_SNAPSHOT(31),
    RESCHEDULE_MANUAL_SNAPSHOT(32),
    DELETE_MANUAL_SNAPSHOT_SCHEDULE(33),
    RESCUE(34),
    END_RESCUE(35),
    BILLING_SUSPEND(36),
    CREATE_REVERSE_DNS_NAME_RECORD(37),
    MERGE_SHOPPER(38),
    CREATE_BACKUP_STORAGE(39), // no longer used, but kept for historical purposes
    DESTROY_BACKUP_STORAGE(40), // no longer used, but kept for historical purposes
    ADD_MONITORING(41),
    RESET_BACKUP_STORAGE_CREDS(42), // no longer used, but kept for historical purposes
    REQUEST_CONSOLE(43),
    SYNC_STATUS(44),
    IMPORT_VM(45),
    ENABLE_WINEXE(46),
    SUBMIT_SUSPEND(47),
    SUBMIT_REINSTATE(48),
    ADD_DOMAIN_MONITORING(49),
    PROCESS_SUSPEND(50),
    PROCESS_REINSTATE(51),
    DELETE_DOMAIN_MONITORING(52),
    REPLACE_DOMAIN_MONITORING(53),
    CREATE_OH_BACKUP(54),
    DESTROY_OH_BACKUP(55),
    RESTORE_OH_BACKUP(56);

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
