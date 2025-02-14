package com.godaddy.vps4.orchestration.vm;

import com.godaddy.vps4.orchestration.ActionRequest;
import com.godaddy.vps4.vm.VirtualMachine;
import org.apache.commons.lang3.builder.ReflectionToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

public class VmActionRequest implements ActionRequest {
    public long actionId;
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
