package com.godaddy.vps4.panopta;

import java.util.List;
import java.util.UUID;

import com.godaddy.vps4.panopta.jdbc.PanoptaCustomerDetails;
import com.godaddy.vps4.panopta.jdbc.PanoptaServerDetails;

public interface PanoptaDataService {
    void createPanoptaCustomer(String shopperId, String customerKey);
    void createPanoptaServer(UUID vmId, String shopperId, PanoptaServer panoptaServer);
    PanoptaCustomerDetails getPanoptaCustomerDetails(String shopperId);
    PanoptaServerDetails getPanoptaServerDetails(UUID vmId);
    List<PanoptaServerDetails> getActivePanoptaServers(String shopperId);
    PanoptaDetail getPanoptaDetails(UUID vmId);
    void setPanoptaServerDestroyed(UUID vmId);
    boolean checkAndSetPanoptaCustomerDestroyed(String shopperId);
    UUID getVmId(String serverKey);
}
