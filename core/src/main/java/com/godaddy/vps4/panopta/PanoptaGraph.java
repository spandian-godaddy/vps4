package com.godaddy.vps4.panopta;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import com.godaddy.vps4.vm.VmMetric;

public class PanoptaGraph {
    public VmMetric type;
    public List<Instant> timestamps;
    public List<Double> values;
    public Map<String, String> metadata;
}
