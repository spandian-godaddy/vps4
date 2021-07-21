package com.godaddy.vps4.panopta;

import java.util.HashMap;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.godaddy.vps4.vm.VmMetric;

public class PanoptaGraphId {
    public VmMetric type;
    public int id;

    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    public Map<String, String> metadata = new HashMap<>();
}