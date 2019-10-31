package com.godaddy.vps4.vm;

import java.util.List;
import java.util.UUID;

public interface VmAlertService {
    List<VmMetricAlert> getVmMetricAlertList(UUID vmId);
    VmMetricAlert getVmMetricAlert(UUID vmId, String metric);
    void disableVmMetricAlert(UUID vmId, String metric);
    void reenableVmMetricAlert(UUID vmId, String metric);
}
