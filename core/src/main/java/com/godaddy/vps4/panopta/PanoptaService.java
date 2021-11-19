package com.godaddy.vps4.panopta;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import com.godaddy.vps4.vm.VmOutage;

public interface PanoptaService {
    PanoptaCustomer createCustomer(String shopperId) throws PanoptaServiceException;

    PanoptaCustomer getCustomer(String shopperId);

    void deleteCustomer(String shopperId);

    PanoptaServer createServer(String shopperId, UUID orionGuid, String ipAddress, String[] templates, String[] tags)
            throws PanoptaServiceException;

    PanoptaServer getServer(UUID vmId);

    List<PanoptaServer> getServers(String shopperId, String ipAddress, UUID orionGuid);

    void deleteServer(UUID vmId);

    void setServerAttributes(UUID vmId, Map<Long, String> attributes);

    List<PanoptaMetricId> getUsageIds(UUID vmId);

    List<PanoptaMetricId> getNetworkIds(UUID vmId);

    List<PanoptaGraph> getUsageGraphs(UUID vmId, String timescale) throws PanoptaServiceException;

    List<PanoptaGraph> getNetworkGraphs(UUID vmId, String timescale) throws PanoptaServiceException;

    void pauseServerMonitoring(UUID vmId);

    void resumeServerMonitoring(UUID vmId, UUID orionGuid);

    PanoptaAvailability getAvailability(UUID vmId, String startTime, String endTime) throws PanoptaServiceException;

    VmOutage getOutage(UUID vmId, long outageId) throws PanoptaServiceException;

    List<VmOutage> getOutages(UUID vmId, boolean activeOnly) throws PanoptaServiceException;

    String getDefaultGroup(String shopperId) throws PanoptaServiceException;
}
