package com.godaddy.vps4.panopta;

import java.util.Map;
import java.util.UUID;

public interface PanoptaService {
    PanoptaCustomer createCustomer(UUID vmId) throws PanoptaServiceException;

    void deleteCustomer(String partnerCustomerKey) throws PanoptaServiceException;

    PanoptaServerMetric getServerMetricsFromPanopta(int agentResourceId, int serverId, String timescale,
                                                    String partnerCustomerKey) throws PanoptaServiceException;

    Map<String, Integer> getAgentResourceIdList(int serverId, String partnerCustomerKey)
            throws PanoptaServiceException;

    PanoptaServer getServer(String partnerCustomerKey) throws PanoptaServiceException;

    void pauseServerMonitoring(UUID vmId);

    void resumeServerMonitoring(UUID vmId);

}
