package com.godaddy.vps4.project;

import java.util.HashMap;
import java.util.Map;

import java.util.Collections;

public enum ProjectPrivilege {

    OWNER(1),
    CREATE(2),
    DELETE(3),
    MANAGE(4),
    CONFIGURE(5)
    ;

    private final int id;

    private ProjectPrivilege(int id) {
        this.id = id;
    }

    public int id() {
        return id;
    }

    private static final Map<Integer, ProjectPrivilege> BY_ID;
    static {
        Map<Integer, ProjectPrivilege> byId = new HashMap<>();
        for (ProjectPrivilege privilege : ProjectPrivilege.values()) {
            byId.put(privilege.id, privilege);
        }
        BY_ID = Collections.unmodifiableMap(byId);
    }

    public static ProjectPrivilege byId(int privilegeId) {
        return BY_ID.get(privilegeId);
    }
}
