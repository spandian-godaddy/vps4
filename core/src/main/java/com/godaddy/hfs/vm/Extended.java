package com.godaddy.hfs.vm;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.apache.commons.lang3.builder.ReflectionToStringBuilder;

public class Extended {
    @JsonProperty("hypervisor_hostname")
    public String hypervisorHostname;
    @JsonProperty("power_state")
    public String powerState;
    @JsonProperty("task_state")
    public String taskState;

    @Override
    public String toString() {
        return ReflectionToStringBuilder.toString(this);
    }
}