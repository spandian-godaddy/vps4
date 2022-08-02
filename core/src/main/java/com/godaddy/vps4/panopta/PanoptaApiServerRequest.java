package com.godaddy.vps4.panopta;

import org.apache.commons.lang3.builder.ReflectionToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

import com.fasterxml.jackson.annotation.JsonProperty;

/*
 * {
 *   "fqdn": "107.180.104.250",
 *   "name": "9c89f5e4-b662-44e8-98e8-75c69c7f5ec8",
 *   "server_group": "https://api2.panopta.com/v2/server_group/406698"
 * }
 */

public class PanoptaApiServerRequest {
    @JsonProperty("fqdn")
    public String fqdn;
    @JsonProperty("name")
    public String name;
    @JsonProperty("server_group")
    public String serverGroup;
    @JsonProperty("tags")
    public String[] tags;

    PanoptaApiServerRequest(String fqdn,
                            String name,
                            String serverGroup,
                            String[] tags) {
        this.fqdn = fqdn;
        this.name = name;
        this.serverGroup = serverGroup;
        this.tags = tags;
    }

    @Override
    public String toString() {
        return ReflectionToStringBuilder.toString(this, ToStringStyle.MULTI_LINE_STYLE);
    }
}
