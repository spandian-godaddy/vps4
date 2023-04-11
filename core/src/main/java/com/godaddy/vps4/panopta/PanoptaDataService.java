package com.godaddy.vps4.panopta;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import com.godaddy.vps4.panopta.jdbc.PanoptaCustomerDetails;
import com.godaddy.vps4.panopta.jdbc.PanoptaServerDetails;

public interface PanoptaDataService {
    void createPanoptaCustomer(String shopperId, String customerKey);
    void createPanoptaServer(UUID vmId, String shopperId, String templateId, PanoptaServer panoptaServer);
    PanoptaCustomerDetails getPanoptaCustomerDetails(String shopperId);
    PanoptaServerDetails getPanoptaServerDetails(UUID vmId);
    List<PanoptaServerDetails> getPanoptaServerDetailsList(String shopperId);
    PanoptaDetail getPanoptaDetails(UUID vmId);
    void setPanoptaServerDestroyed(UUID vmId);
    void setAllPanoptaServersOfCustomerDestroyed(String shopperId);
    boolean checkAndSetPanoptaCustomerDestroyed(String shopperId);
    UUID getVmId(String serverKey);
    List<String> getPanoptaActiveAdditionalFqdns(UUID vmId);
    void addPanoptaAdditionalFqdn(String fqdn, long panoptaServerId);
    boolean activeAdditionalFqdnExistsForServer(String fqdn, long panoptaServerId);
    void deletePanoptaAdditionalFqdn(String fqdn, long panoptaServerId);
    void deleteVirtualMachineAdditionalFqdns(UUID vmId);
    Map<String, Instant> getPanoptaAdditionalFqdnWithValidOn(UUID vmId);
}
