package com.godaddy.vps4.snapshot;

import static java.util.Arrays.stream;
import static java.util.stream.Collectors.toMap;

import java.util.Map;

public enum SnapshotType {
    ON_DEMAND(1),
    AUTOMATIC(2);

    private final int typeId;

    SnapshotType(int typeId) {
        this.typeId = typeId;
    }

    private final static Map<Integer, SnapshotType> map = stream(SnapshotType.values())
            .collect(toMap(type -> type.typeId, type -> type));

    public static SnapshotType valueOf(int typeId) {
        return map.get(typeId);
    }

    public int getSnapshotTypeId() {
        return typeId;
    }
}
