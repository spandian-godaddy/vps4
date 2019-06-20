package com.godaddy.vps4.orchestration.hfs.dns;

import org.apache.commons.lang3.builder.ReflectionToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

import com.godaddy.vps4.orchestration.ActionRequest;
import com.godaddy.vps4.vm.VirtualMachine;

public class Vps4ReverseDnsNameRecordRequest implements ActionRequest {
    public long actionId;
    public String reverseDnsName;
    public VirtualMachine virtualMachine;

    @Override
    public long getActionId() {
        return actionId;
    }

    @Override
    public void setActionId(long actionId) {
        this.actionId = actionId;
    }

    public String toString() {
        return ReflectionToStringBuilder.toString(this, ToStringStyle.SHORT_PREFIX_STYLE);
    }
}
