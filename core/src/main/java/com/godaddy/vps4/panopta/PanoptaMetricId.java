package com.godaddy.vps4.panopta;

import java.util.HashMap;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonProperty;

public class PanoptaMetricId {
    public long id;
    public long typeId;
    public String serverInterface;
    public String status;

    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    public Map<String, String> metadata = new HashMap<>();
}