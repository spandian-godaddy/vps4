package com.godaddy.vps4.web.support;

import com.godaddy.vps4.vm.Action;

import gdg.hfs.orchestration.CommandState;

public class SupportAction {

    public final Action action;
    public final CommandState orchestrationCommand;
    

    public SupportAction(Action action,  CommandState orchestrationCommand){
        this.action = action;
        this.orchestrationCommand = orchestrationCommand;
    }


    public String toString(){
        return "Action [action: " + action
                + " command: " + orchestrationCommand + "]";
    }

}
