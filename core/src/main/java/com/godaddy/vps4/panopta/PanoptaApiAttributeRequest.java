package com.godaddy.vps4.panopta;

import com.fasterxml.jackson.annotation.JsonProperty;

/*
 * {
 *   "server_attribute_type": null,
 *   "value": null
 * }
 */
public class PanoptaApiAttributeRequest {
    private final String serverAttributeTypeUrl;
    private final String value;

    public PanoptaApiAttributeRequest(long typeId, String value) {
        this.serverAttributeTypeUrl = "https://api2.panopta.com/v2/server_attribute_type/" + typeId;
        this.value = value;
    }

    @JsonProperty("server_attribute_type")
    public String getServerAttributeTypeUrl() {
        return serverAttributeTypeUrl;
    }

    @JsonProperty("value")
    public String getValue() {
        return value;
    }
}
