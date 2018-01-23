package com.godaddy.vps4.appmonitors;

import java.util.UUID;

public class SnapshotActionData {
    public String actionId;
    public UUID commandId;
    public UUID snapshotId;

    public SnapshotActionData(String actionId, UUID commandId, UUID snapshotId) {
        this.actionId = actionId;
        this.commandId = commandId;
        this.snapshotId = snapshotId;
    }
}
