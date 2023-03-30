package com.godaddy.vps4.web.monitoring;

import org.apache.commons.lang3.builder.ReflectionToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

public class VmOutageRequest {
    public String timestamp;

    // Empty constructor required for Jackson
    public VmOutageRequest() {}

    public VmOutageRequest(String timestamp) {
        this.timestamp = timestamp;
    }

    public String toString() {
        return ReflectionToStringBuilder.toString(this, ToStringStyle.SHORT_PREFIX_STYLE);
    }
}
