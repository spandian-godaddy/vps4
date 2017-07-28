package com.godaddy.vps4.web.vm;

import com.godaddy.vps4.vm.Action;
import gdg.hfs.orchestration.CommandState;

public class VmActionWithDetails extends VmAction {

    public final CommandState orchestrationCommand;
    public String message;

    public VmActionWithDetails(Action a, CommandState orchestrationCommand){
        super(a);
        this.orchestrationCommand = orchestrationCommand;
    }

    public VmActionWithDetails(Action a, CommandState orchestrationCommand, String message){
        this(a, orchestrationCommand);
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
