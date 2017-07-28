package com.godaddy.vps4.web.vm;

import com.godaddy.vps4.vm.Action;
import com.godaddy.vps4.vm.ActionStatus;
import com.godaddy.vps4.vm.ActionType;

import java.time.Instant;
import java.util.UUID;

public class VmAction {

    public final long id;
    public final UUID virtualMachineId;
    public final ActionType type;
    public final long vps4UserId;
    public final String request;
    public final String state;
    public final String response;
    public final ActionStatus status;
    public final Instant created;
    public final String note;
    public final UUID commandId;

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
        this.note = a.note;
        this.commandId = a.commandId;
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
                + " note: " + note
                + " commandId: " + commandId + "]";
    }

}
