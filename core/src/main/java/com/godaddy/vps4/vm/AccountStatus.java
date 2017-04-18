package com.godaddy.vps4.vm;

import static java.util.Arrays.stream;
import static java.util.stream.Collectors.toMap;

import java.util.Map;

public enum AccountStatus {
    ACTIVE(1), SUSPENDED(2), ABUSE_SUSPENDED(3), REMOVED(4);

    private final int statusId;

    AccountStatus(int statusId) {
        this.statusId = statusId;
    }

    private final static Map<Integer, AccountStatus> map = stream(AccountStatus.values())
            .collect(toMap(status -> status.statusId, status -> status));

    public static AccountStatus valueOf(int statusId) {
        return map.get(statusId);
    }

    public int getAccountStatusId() {
        return statusId;
    }
}