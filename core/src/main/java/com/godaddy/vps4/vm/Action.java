package com.godaddy.vps4.vm;

import java.time.Instant;

public class Action {

    public final long id;
    public final long virtualMachineId;
    public final ActionType type;
    public final long vps4UserId;
    public final String request;
    public final String response;
    public final ActionStatus status;
    public final Instant created;
    public final String note;

    public Action(long id, long virtualMachineId, ActionType type, long vps4UserId, String request, String response, ActionStatus status, Instant created, String note){
        this.id = id;
        this.virtualMachineId = virtualMachineId;
        this.type = type;
        this.vps4UserId = vps4UserId;
        this.request = request;
        this.response = response;
        this.status = status;
        this.created = created;
        this.note = note;
    }


    public String toString(){
        return "Action [id: " + id
                + " virtualMachineId: " + virtualMachineId
                + " actionType: " + type
                + " vps4UserId: " + vps4UserId
                + " request: " + request
                + " response: " + response
                + " status: " + status
                + " created: " + created
                + " note: " + note + "]";
    }

}
