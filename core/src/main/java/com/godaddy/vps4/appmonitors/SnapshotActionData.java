package com.godaddy.vps4.appmonitors;

import java.util.UUID;

public class SnapshotActionData {
    public String actionId;
    public UUID commandId;
    public UUID snapshotId;
    public UUID vmId;
    public String actionType;
    public String actionStatus;
    public String createdDate;

    public SnapshotActionData(String actionId, UUID commandId, UUID snapshotId, UUID vmId, String actionType,
                              String actionStatus, String createdDate) {
        this.actionId = actionId;
        this.commandId = commandId;
        this.snapshotId = snapshotId;
        this.vmId = vmId;
        this.actionType = actionType;
        this.actionStatus = actionStatus;
        this.createdDate = createdDate;
    }
}
