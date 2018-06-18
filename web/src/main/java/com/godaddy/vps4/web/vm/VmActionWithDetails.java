package com.godaddy.vps4.web.vm;

import com.godaddy.vps4.vm.Action;
import com.godaddy.vps4.vm.VmAction;

import org.apache.commons.lang3.builder.ReflectionToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

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
        return ReflectionToStringBuilder.toString(this, ToStringStyle.SHORT_PREFIX_STYLE);
    }

}
