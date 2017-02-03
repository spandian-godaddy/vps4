package com.godaddy.vps4.vm;

import java.time.Instant;
import java.util.UUID;

public class Action {

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

    public Action(long id, UUID virtualMachineId, ActionType type, long vps4UserId, String request, String state, String response, ActionStatus status, Instant created, String note, UUID commandId){
        this.id = id;
        this.virtualMachineId = virtualMachineId;
        this.type = type;
        this.vps4UserId = vps4UserId;
        this.request = request;
        this.state = state;
        this.response = response;
        this.status = status;
        this.created = created;
        this.note = note;
        this.commandId = commandId;
    }


    public String toString(){
        return "Action [id: " + id
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
