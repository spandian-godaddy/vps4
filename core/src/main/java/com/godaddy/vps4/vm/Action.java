package com.godaddy.vps4.vm;

import java.time.Instant;
import java.util.UUID;

import org.apache.commons.lang3.builder.ReflectionToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

public class Action {

    public long id;
    public UUID resourceId;
    public ActionType type;
    public String request;
    public String state;
    public String response;
    public ActionStatus status;
    public Instant created;
    public Instant completed;
    public String note;
    public UUID commandId;
    public String initiatedBy;

    
    public Action(long id, UUID resourceId, ActionType type, String request, String state, String response,
            ActionStatus status, Instant created, Instant completed, String note, UUID commandId, String initiatedBy) {
        this.id = id;
        this.resourceId = resourceId;
        this.type = type;
        this.request = request;
        this.state = state;
        this.response = response;
        this.status = status;
        this.created = created;
        this.completed = completed;
        this.note = note;
        this.commandId = commandId;
        this.initiatedBy = initiatedBy;
    }

    public String toString() {
        return ReflectionToStringBuilder.toString(this, ToStringStyle.SHORT_PREFIX_STYLE);
    }

}
