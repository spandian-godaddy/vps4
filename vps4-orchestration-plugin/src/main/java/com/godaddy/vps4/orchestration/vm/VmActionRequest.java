package com.godaddy.vps4.orchestration.vm;

import com.godaddy.vps4.orchestration.ActionRequest;
import org.apache.commons.lang3.builder.ReflectionToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

public class VmActionRequest implements ActionRequest {
    public long actionId;
    public long hfsVmId;

    @Override
    public long getActionId() {
        return actionId;
    }

    @Override
    public void setActionId(long actionId) {
        this.actionId = actionId;
    }

    public String toString() {
        return ReflectionToStringBuilder.toString(this, ToStringStyle.MULTI_LINE_STYLE);
    }

}
