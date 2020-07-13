package com.godaddy.hfs.vm;

import com.fasterxml.jackson.annotation.JsonProperty;

public class Extended {
    @JsonProperty("hypervisor_hostname")
    public String hypervisorHostname;

    @Override
    public String toString() {
        return "Extended [hypervisor_hostname=" + hypervisorHostname + "]";
    }
}