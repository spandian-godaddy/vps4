package com.godaddy.vps4.panopta;

import java.util.List;
import java.util.UUID;

public interface PanoptaService {
    PanoptaCustomer createCustomer(String shopperId) throws PanoptaServiceException;

    PanoptaCustomer getCustomer(String shopperId);

    void deleteCustomer(String shopperId);

    PanoptaServer createServer(String shopperId, UUID orionGuid, String ipAddress, String[] templates)
            throws PanoptaServiceException;

    List<PanoptaGraphId> getUsageIds(UUID vmId);

    List<PanoptaGraphId> getNetworkIds(UUID vmId);

    List<PanoptaGraph> getUsageGraphs(UUID vmId, String timescale) throws PanoptaServiceException;

    List<PanoptaGraph> getNetworkGraphs(UUID vmId, String timescale) throws PanoptaServiceException;

    List<PanoptaServer> getActiveServers(String shopperId);

    List<PanoptaServer> getSuspendedServers(String shopperId);

    void pauseServerMonitoring(UUID vmId);

    void resumeServerMonitoring(UUID vmId);

    void removeServerMonitoring(UUID vmId);

    void removeServerMonitoring(long panoptaServerId, String partnerCustomerKey);

    PanoptaAvailability getAvailability(UUID vmId, String startTime, String endTime) throws PanoptaServiceException;

    String getDefaultGroup(String shopperId) throws PanoptaServiceException;
}
