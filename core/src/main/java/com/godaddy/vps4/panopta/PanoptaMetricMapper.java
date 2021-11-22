package com.godaddy.vps4.panopta;

import java.util.Map;

import com.godaddy.vps4.vm.VmMetric;
import com.google.inject.Inject;

public class PanoptaMetricMapper {
    private final Map<Long, VmMetric> map;
    
    @Inject
    public PanoptaMetricMapper(PanoptaMetricService panoptaMetricService) {
        map = panoptaMetricService.getAllMetrics();
    }

    public VmMetric getVmMetric(long panoptaTypeId) {
        if (map.containsKey(panoptaTypeId)) {
            return map.get(panoptaTypeId);
        }
        return VmMetric.UNKNOWN;
    }
}
