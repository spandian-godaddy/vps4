package com.godaddy.vps4.vm;

import java.time.Instant;
import java.util.UUID;

public class Action {

    public final long id;
    public final UUID resourceId;
    public final ActionType type;
    public final long vps4UserId;
    public final String request;
    public final String state;
    public final String response;
    public final ActionStatus status;
    public final Instant created;
    public final Instant completed;
    public final String note;
    public final UUID commandId;

    public Action(long id, UUID resourceId, ActionType type, long vps4UserId,
                  String request, String state, String response, ActionStatus status,
                  Instant created, Instant completed, String note, UUID commandId){
        this.id = id;
        this.resourceId = resourceId;
        this.type = type;
        this.vps4UserId = vps4UserId;
        this.request = request;
        this.state = state;
        this.response = response;
        this.status = status;
        this.created = created;
        this.completed = completed;
        this.note = note;
        this.commandId = commandId;
    }


    public String toString(){
        return "Action [id: " + id
                + " resourceId: " + resourceId
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
