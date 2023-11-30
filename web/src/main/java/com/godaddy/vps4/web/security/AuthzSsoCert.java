package com.godaddy.vps4.web.security;

import com.fasterxml.jackson.annotation.JsonProperty;

public class AuthzSsoCert {
    @JsonProperty("name")
    public String name;

    @JsonProperty("cn")
    public String cn;

    @JsonProperty("role")
    public String role;
}
