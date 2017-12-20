package com.godaddy.vps4.vm;

import static java.util.Arrays.stream;
import static java.util.stream.Collectors.toMap;

import java.util.Map;

public enum ActionStatus {
    NEW(1), IN_PROGRESS(2), COMPLETE(3), ERROR(4), CANCELLED(5), INVALID(6);

    private final int statusId;

    ActionStatus(int statusId) {
        this.statusId = statusId;
    }

    private final static Map<Integer, ActionStatus> map = stream(ActionStatus.values())
            .collect(toMap(status -> status.statusId, status -> status));

    public static ActionStatus valueOf(int statusId) {
        return map.get(statusId);
    }

    public int getStatusId() {
        return statusId;
    }

}
