package com.godaddy.vps4.vm;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public interface VmOutageService {
    List<VmOutage> getVmOutageList(UUID vmId);
    List<VmOutage> getVmOutageList(UUID vmId, VmMetric metric);
    List<VmOutage> getVmOutageList(UUID vmId, VmMetric metric, boolean activeOnly);
    List<VmOutage> getVmOutageList(long panoptaOutageId);
    VmOutage getVmOutage(int outageId);
    VmOutage getVmOutage(UUID vmId, VmMetric metric, long panoptaOutageId);
    int newVmOutage(UUID vmId, VmMetric metric, Instant startDate, String reason, long panoptaOutageId);
    void clearVmOutage(int outageId, Instant endDate);
    void clearAllActiveOutagesByMetric(UUID vmId, VmMetric metric, Instant endDate);
}
