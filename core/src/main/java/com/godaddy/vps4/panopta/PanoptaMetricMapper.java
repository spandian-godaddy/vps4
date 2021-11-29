package com.godaddy.vps4.panopta;

import java.util.Map;

import com.godaddy.vps4.vm.VmMetric;
import com.google.inject.Inject;

public class PanoptaMetricMapper {
    private final Map<Long, VmMetric> map;
    private final PanoptaMetricService panoptaMetricService;

    @Inject
    public PanoptaMetricMapper(PanoptaMetricService panoptaMetricService) {
        map = panoptaMetricService.getAllMetrics();
        this.panoptaMetricService = panoptaMetricService;
    }

    public VmMetric getVmMetric(long panoptaTypeId) {
        if (map.containsKey(panoptaTypeId)) {
            return map.get(panoptaTypeId);
        }
        return VmMetric.UNKNOWN;
    }

    public Long getMetricTypeId(VmMetric metric, int osTypeId) {
        return panoptaMetricService.getMetricTypeId(metric.name(), osTypeId);
    }
}
