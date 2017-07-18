package com.godaddy.vps4.snapshot;

import java.util.Map;

import static java.util.Arrays.stream;
import static java.util.stream.Collectors.toMap;

public enum SnapshotStatus {
    NEW(1),
    IN_PROGRESS(2),
    COMPLETE(3),
    ERROR(4),
    DESTROYED(5);

    private final int typeId;

    SnapshotStatus(int typeId) {
        this.typeId = typeId;
    }

    private final static Map<Integer, SnapshotStatus> map = stream(SnapshotStatus.values())
            .collect(toMap(type -> type.typeId, type -> type));

    public static SnapshotStatus valueOf(int typeId) {
        return map.get(typeId);
    }

    public int getSnapshotStatusId() {
        return typeId;
    }
}
