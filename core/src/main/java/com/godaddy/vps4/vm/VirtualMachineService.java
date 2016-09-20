package com.godaddy.vps4.vm;

import java.util.List;
import java.util.UUID;

public interface VirtualMachineService {

    List<VirtualMachine> listVirtualMachines(long projectId);

    VirtualMachine getVirtualMachine(long vmId);
    
    VirtualMachine getVirtualMachine(UUID orionGuid);
    
    VirtualMachineSpec getSpec(String name);
    
    VirtualMachineSpec getSpec(int tier);

    /**
     *
     * @param vmId - the VM ID from the VM Vertical Service
     * @param projectId
     * @param spec
     * @param name
     * @return
     */
    VirtualMachine createVirtualMachine(long vmId, long projectId, String spec, String name);

    // updateStatus(long vmId, int newStatus)

    void destroyVirtualMachine(UUID uuid);  // (just updates status/sets validUntil, destroy is accomplished on backend)

	void createVirtualMachine(UUID orionGuid,
							  long projectId,
							  int osTypeId, 
							  int controlPanelId, 
							  int specId,
							  int managedLevel);

	void updateVirtualMachine(UUID orionGuid, String name, String image, int dataCenterId);


}
