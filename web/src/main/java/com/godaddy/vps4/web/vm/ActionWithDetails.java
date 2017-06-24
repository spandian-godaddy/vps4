package com.godaddy.vps4.web.vm;

import com.godaddy.vps4.vm.Action;

import gdg.hfs.orchestration.CommandState;

public class ActionWithDetails extends Action {

    public final CommandState orchestrationCommand;
    public String message;

    public ActionWithDetails(Action a,  CommandState orchestrationCommand){
        super(a.id, a.virtualMachineId, a.type, a.vps4UserId, a.request, a.state, a.response,
                a.status, a.created, a.note, a.commandId);
        this.orchestrationCommand = orchestrationCommand;
    }

    public ActionWithDetails(Action a,  CommandState orchestrationCommand, String message){
        super(a.id, a.virtualMachineId, a.type, a.vps4UserId, a.request, a.state, a.response,
                a.status, a.created, a.note, a.commandId);
        this.orchestrationCommand = orchestrationCommand;
        this.message = message;
    }

    @Override
    public String toString() {
        return "ActionWithDetails [orchestrationCommand=" + orchestrationCommand + ", message=" + message + ", id=" + id
                + ", virtualMachineId=" + virtualMachineId + ", type=" + type + ", vps4UserId=" + vps4UserId
                + ", request=" + request + ", state=" + state + ", response=" + response + ", status=" + status
                + ", created=" + created + ", note=" + note + ", commandId=" + commandId + "]";
    }

}
