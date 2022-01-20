package com.godaddy.vps4.panopta;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.godaddy.vps4.vm.VmMetric;

public class PanoptaGraph {
    public VmMetric type;
    public List<Instant> timestamps;
    public List<Double> values;
    public Map<String, String> metadata;
    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    public String serverInterface;
}
