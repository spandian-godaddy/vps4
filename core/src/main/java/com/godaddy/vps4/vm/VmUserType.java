package com.godaddy.vps4.vm;

import java.util.Map;

import static java.util.Arrays.stream;
import static java.util.stream.Collectors.toMap;

public enum VmUserType {
    CUSTOMER(1),
    SUPPORT(2);

    private final int typeId;

    VmUserType(int typeId) {
        this.typeId = typeId;
    }

    private final static Map<Integer, VmUserType> map = stream(VmUserType.values())
            .collect(toMap(type -> type.typeId, type -> type));

    public static VmUserType valueOf(int typeId) {
        return map.get(typeId);
    }

    public int getVmUserTypeId() {
        return typeId;
    }
}
