package com.godaddy.vps4.panopta;

import java.util.List;
import java.util.UUID;

public interface PanoptaService {
    PanoptaCustomer createCustomer(UUID vmId) throws PanoptaServiceException;

    void deleteCustomer(UUID vmId);

    List<PanoptaGraphId> getUsageIds(UUID vmId);

    List<PanoptaGraphId> getNetworkIds(UUID vmId);

    List<PanoptaGraph> getUsageGraphs(UUID vmId, String timescale) throws PanoptaServiceException;

    List<PanoptaGraph> getNetworkGraphs(UUID vmId, String timescale) throws PanoptaServiceException;

    PanoptaServer getServer(String shopperId, String serverKey) throws PanoptaServiceException;

    void pauseServerMonitoring(UUID vmId);

    void resumeServerMonitoring(UUID vmId);

    void removeServerMonitoring(UUID vmId);

    PanoptaAvailability getAvailability(UUID vmId, String startTime, String endTime) throws PanoptaServiceException;

    PanoptaOutage getOutage(UUID vmId, String startTime, String endTime, int limit, int offset)
            throws PanoptaServiceException;
}
