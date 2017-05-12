package com.godaddy.vps4.web.support;

import com.godaddy.vps4.vm.Action;

import gdg.hfs.orchestration.CommandState;

public class SupportAction {

    public final Action action;
    public final CommandState orchestrationCommand;
    public String message;
    

    public SupportAction(Action action,  CommandState orchestrationCommand){
        this.action = action;
        this.orchestrationCommand = orchestrationCommand;
    }


    public SupportAction(Action action, CommandState command, String message) {
        this.action = action;
        this.orchestrationCommand = command;
        this.message = message;
    }

    public String toString(){
        return "Action [action: " + action
                + " command: " + orchestrationCommand
                + " message: " + message + "]";
    }

}
