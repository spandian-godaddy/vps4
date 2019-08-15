package com.godaddy.vps4.panopta;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * {
 *   "fqdn": "panopta.cent7.a2",
 *   "name": "panopta.cent7.a2",
 *   "server_group": "https://api2.panopta.com/v2/server_group/346858",
 *   "status": "suspended"
 * }
 */

public class PanoptaApiUpdateServerRequest {
    public String fqdn;
    public String name;
    @JsonProperty("server_group")
    public String serverGroup;
    public String status;
}
