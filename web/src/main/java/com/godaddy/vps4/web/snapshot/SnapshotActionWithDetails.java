package com.godaddy.vps4.web.snapshot;

import com.godaddy.vps4.vm.Action;
import gdg.hfs.orchestration.CommandState;

public class SnapshotActionWithDetails extends SnapshotAction {

    public final CommandState orchestrationCommand;
    public String message;

    public SnapshotActionWithDetails(Action a, CommandState orchestrationCommand){
        super(a);
        this.orchestrationCommand = orchestrationCommand;
    }

    public SnapshotActionWithDetails(Action a, CommandState orchestrationCommand, String message){
        this(a, orchestrationCommand);
        this.message = message;
    }

    @Override
    public String toString() {
        return "SnapshotActionWithDetails [orchestrationCommand=" + orchestrationCommand + ", message=" + message + ", id=" + id
                + ", snapshotId=" + snapshotId + ", type=" + type + ", vps4UserId=" + vps4UserId
                + ", request=" + request + ", state=" + state + ", response=" + response + ", status=" + status
                + ", created=" + created + ", note=" + note + ", commandId=" + commandId + "]";
    }

}
