package com.godaddy.vps4.web.snapshot;

import com.godaddy.vps4.snapshot.SnapshotAction;
import com.godaddy.vps4.vm.Action;

import org.apache.commons.lang3.builder.ReflectionToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

import gdg.hfs.orchestration.CommandState;

public class SnapshotActionWithDetails extends SnapshotAction {

    public final CommandState orchestrationCommand;
    public String message;

    public SnapshotActionWithDetails(Action a, CommandState orchestrationCommand, boolean userIsEmployee){
        super(a, userIsEmployee);
        this.orchestrationCommand = orchestrationCommand;
    }

    public SnapshotActionWithDetails(Action a, CommandState orchestrationCommand, String message, boolean userIsEmployee){
        this(a, orchestrationCommand, userIsEmployee);
        this.message = message;
    }

    @Override
    public String toString() {
        return ReflectionToStringBuilder.toString(this, ToStringStyle.SHORT_PREFIX_STYLE);
    }

}
