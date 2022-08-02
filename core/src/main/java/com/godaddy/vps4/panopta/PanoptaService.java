package com.godaddy.vps4.panopta;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import com.godaddy.vps4.vm.VmMetric;
import com.godaddy.vps4.vm.VmOutage;

public interface PanoptaService {
    PanoptaCustomer createCustomer(String shopperId) throws PanoptaServiceException;

    PanoptaCustomer getCustomer(String shopperId);

    void deleteCustomer(String shopperId);

    PanoptaServer createServer(String shopperId, UUID orionGuid, String ipAddress, String[] tags)
            throws PanoptaServiceException;

    void applyTemplates(long serverId, String partnerCustomerKey, String[] templates) throws PanoptaServiceException;

    PanoptaServer getServer(UUID vmId);

    void addAdditionalFqdnToServer(UUID vmId, String additionalFqdn) throws PanoptaServiceException;

    void deleteAdditionalFqdnFromServer(UUID vmId, String additionalFqdn) throws PanoptaServiceException;

    List<PanoptaServer> getServers(String shopperId, String ipAddress, UUID orionGuid);

    void deleteServer(UUID vmId);

    void setServerAttributes(UUID vmId, Map<Long, String> attributes);

    List<PanoptaMetricId> getUsageIds(UUID vmId);

    void addNetworkService(UUID vmId, VmMetric metric, String additionalFqdn, int osTypeId, boolean isManaged) throws PanoptaServiceException;

    void deleteNetworkService(UUID vmId, long networkServiceId) throws PanoptaServiceException;

    List<PanoptaMetricId> getNetworkIds(UUID vmId);

    PanoptaMetricId getNetworkIdOfAdditionalFqdn(UUID vmId, String fqdn) throws PanoptaServiceException;

    List<PanoptaGraph> getUsageGraphs(UUID vmId, String timescale) throws PanoptaServiceException;

    List<PanoptaGraph> getNetworkGraphs(UUID vmId, String timescale) throws PanoptaServiceException;

    void pauseServerMonitoring(UUID vmId);

    void resumeServerMonitoring(UUID vmId, UUID orionGuid);

    PanoptaAvailability getAvailability(UUID vmId, String startTime, String endTime) throws PanoptaServiceException;

    VmOutage getOutage(UUID vmId, long outageId) throws PanoptaServiceException;

    List<VmOutage> getOutages(UUID vmId, boolean activeOnly) throws PanoptaServiceException;

    String getDefaultGroup(String shopperId) throws PanoptaServiceException;

    List<PanoptaDomain> getAdditionalDomains(UUID vmId);
}
