package com.godaddy.vps4.orchestration;

import org.apache.commons.lang3.builder.ReflectionToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

import java.util.UUID;

public class Vps4ActionRequest implements ActionRequest {
    public long actionId;
    public UUID vmId;

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
