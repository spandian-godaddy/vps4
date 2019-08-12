package com.godaddy.vps4.panopta;

import java.util.UUID;

public interface PanoptaDataService {
    void createPanoptaDetails(UUID vmId, PanoptaCustomer panoptaCustomer, PanoptaServer panoptaServer);
    void setServerDestroyedInPanopta(UUID vmId);
    PanoptaDetail getPanoptaDetails(UUID vmId);
}
