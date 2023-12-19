package com.godaddy.vps4.firewall.model;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class FirewallClientUpdateRequest {
    public String cacheLevel;
    public String bypassWAF;
}
