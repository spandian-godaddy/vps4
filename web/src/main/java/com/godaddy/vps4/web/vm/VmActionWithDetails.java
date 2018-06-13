package com.godaddy.vps4.web.vm;

import com.godaddy.vps4.vm.Action;
import com.godaddy.vps4.vm.VmAction;

import gdg.hfs.orchestration.CommandState;

public class VmActionWithDetails extends VmAction {

    public final CommandState orchestrationCommand;
    public String message;

    public VmActionWithDetails(Action a, CommandState orchestrationCommand, boolean isUserEmployee){
        super(a, isUserEmployee);
        this.orchestrationCommand = orchestrationCommand;
    }

    public VmActionWithDetails(Action a, CommandState orchestrationCommand, String message, boolean isUserEmployee){
        this(a, orchestrationCommand, isUserEmployee);
        this.message = message;
    }

    @Override
    public String toString() {
        return "VmActionWithDetails [orchestrationCommand=" + orchestrationCommand + ", message=" + message + ", id=" + id
                + ", virtualMachineId=" + virtualMachineId + ", type=" + type + ", vps4UserId=" + vps4UserId
                + ", request=" + request + ", state=" + state + ", response=" + response + ", status=" + status
                + ", created=" + created + ", note=" + note + ", commandId=" + commandId + "]";
    }

}
