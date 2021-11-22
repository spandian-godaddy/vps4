package com.godaddy.vps4.panopta;

import java.util.Map;

import com.godaddy.vps4.vm.VmMetric;

public interface PanoptaMetricService {
    Map<Long, VmMetric> getAllMetrics();
    Long getMetricTypeId(String metric, int osTypeId);
}
