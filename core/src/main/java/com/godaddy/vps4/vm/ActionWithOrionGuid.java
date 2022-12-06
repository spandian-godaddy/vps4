package com.godaddy.vps4.vm;

import org.apache.commons.lang3.builder.ReflectionToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

import java.time.Instant;
import java.util.UUID;

public class ActionWithOrionGuid {

    public UUID orionGuid;
    public Action action;

    public ActionWithOrionGuid(
            Action action,
            UUID orionGuid) {
        this.action = action;
        this.orionGuid = orionGuid;
    }

    public String toString() {
        return ReflectionToStringBuilder.toString(this, ToStringStyle.SHORT_PREFIX_STYLE);
    }
}
