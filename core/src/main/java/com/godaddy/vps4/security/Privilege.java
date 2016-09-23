package com.godaddy.vps4.security;

import java.util.HashMap;
import java.util.Map;

import java.util.Collections;

public enum Privilege {

    MANAGE_USERS(6),
    VIEW_USER_SERVICE_GROUPS(7),
    MANAGE_USER_SERVICE_GROUPS(8)
    ;

    private final int id;

    private Privilege(int id) {
        this.id = id;
    }

    public int id() {
        return id;
    }

    private static final Map<Integer, Privilege> BY_ID;
    static {
        Map<Integer, Privilege> byId = new HashMap<Integer, Privilege>();
        for (Privilege privilege : Privilege.values()) {
            byId.put(privilege.id, privilege);
        }
        BY_ID = Collections.unmodifiableMap(byId);
    }

    public static Privilege byId(int privilegeId) {
        return BY_ID.get(privilegeId);
    }

}
