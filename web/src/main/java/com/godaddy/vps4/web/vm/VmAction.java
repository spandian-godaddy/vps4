package com.godaddy.vps4.web.vm;

import com.godaddy.vps4.vm.Action;
import com.godaddy.vps4.vm.ActionStatus;
import com.godaddy.vps4.vm.ActionType;

import java.time.Instant;
import java.util.UUID;

public class VmAction {

    public long id;
    public UUID virtualMachineId;
    public ActionType type;
    public long vps4UserId;
    public String request;
    public String state;
    public String response;
    public ActionStatus status;
    public Instant created;
    public Instant completed;
    public String note;
    public UUID commandId;

    public VmAction(Action a){
        this.id = a.id;
        this.virtualMachineId = a.resourceId;
        this.type = a.type;
        this.vps4UserId = a.vps4UserId;
        this.request = a.request;
        this.state = a.state;
        this.response = a.response;
        this.status = a.status;
        this.created = a.created;
        this.completed = a.completed;
        this.note = a.note;
        this.commandId = a.commandId;
    }

    // This is for jackson so it can deserialize
    public VmAction() {
    }

    @Override
    public String toString(){
        return "VmAction [id: " + id
                + " virtualMachineId: " + virtualMachineId
                + " actionType: " + type
                + " vps4UserId: " + vps4UserId
                + " request: " + request
                + " state: " + state
                + " response: " + response
                + " status: " + status
                + " created: " + created
                + " completed: " + completed
                + " note: " + note
                + " commandId: " + commandId + "]";
    }

}
