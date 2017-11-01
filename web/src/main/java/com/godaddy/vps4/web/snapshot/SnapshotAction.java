package com.godaddy.vps4.web.snapshot;

import com.godaddy.vps4.vm.Action;
import com.godaddy.vps4.vm.ActionStatus;
import com.godaddy.vps4.vm.ActionType;

import java.time.Instant;
import java.util.UUID;

public class SnapshotAction {

    public long id;
    public UUID snapshotId;
    public ActionType type;
    public long vps4UserId;
    public String request;
    public String state;
    public String response;
    public ActionStatus status;
    public Instant created;
    public String note;
    public UUID commandId;

    public SnapshotAction(Action a){
        this.id = a.id;
        this.snapshotId = a.resourceId;
        this.type = a.type;
        this.vps4UserId = a.vps4UserId;
        this.request = a.request;
        this.state = a.state;
        this.response = a.response;
        this.status = a.status;
        this.created = a.created;
        this.note = a.note;
        this.commandId = a.commandId;
    }

    // This is for jackson so it can deserialize
    public SnapshotAction() {
    }

    @Override
    public String toString(){
        return "VmAction [id: " + id
                + " snapshotId: " + snapshotId
                + " actionType: " + type
                + " vps4UserId: " + vps4UserId
                + " request: " + request
                + " state: " + state
                + " response: " + response
                + " status: " + status
                + " created: " + created
                + " note: " + note
                + " commandId: " + commandId + "]";
    }

}
