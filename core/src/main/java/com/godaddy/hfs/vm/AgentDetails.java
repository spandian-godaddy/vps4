package com.godaddy.hfs.vm;

import com.fasterxml.jackson.annotation.JsonProperty;

public class AgentDetails {

    @JsonProperty("agent_status")
    public String agentStatus;
    public String version;

}
