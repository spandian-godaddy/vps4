package com.godaddy.vps4.vm;

import com.fasterxml.jackson.annotation.JsonFilter;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.godaddy.vps4.vm.Action;
import com.godaddy.vps4.vm.ActionStatus;
import com.godaddy.vps4.vm.ActionType;

import org.apache.commons.lang3.builder.ReflectionToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

import java.time.Instant;
import java.util.UUID;

@JsonFilter("actionInitiatedByFilter")
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
    public String initiatedBy;
    @JsonIgnore
    public boolean isRequesterEmployee;

    public VmAction(Action a, boolean isUserEmployee){
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
        this.initiatedBy = a.initiatedBy;
        this.isRequesterEmployee = isUserEmployee;
    }

    // This is for jackson so it can deserialize
    public VmAction() {
    }

    @Override
    public String toString(){
        return ReflectionToStringBuilder.toString(this, ToStringStyle.SHORT_PREFIX_STYLE);
    }

}
