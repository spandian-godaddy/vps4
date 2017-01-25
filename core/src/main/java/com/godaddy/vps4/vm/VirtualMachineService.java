package com.godaddy.vps4.vm;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import com.godaddy.vps4.security.Vps4User;

public interface VirtualMachineService {

    List<VirtualMachine> getVirtualMachinesForProject(long projectId);

    VirtualMachine getVirtualMachine(long hfsVmId);

    VirtualMachine getVirtualMachineByOrionGuid(UUID orionGuid);

    VirtualMachine getVirtualMachine(UUID vmId);

    VirtualMachineSpec getSpec(String name);

    VirtualMachineSpec getSpec(int tier);

    /**
     * @param vmId - the VM ID from the VM Vertical Service
     * @param projectId
     * @param spec
     * @param name
     * @return
     */
//    VirtualMachine createVirtualMachine(long vmId, long projectId, String spec, String name);

    // updateStatus(long vmId, int newStatus)

    void destroyVirtualMachine(long vmId); // (just updates status/sets validUntil, destroy is accomplished on backend)

    void createVirtualMachineCredit(UUID orionGuid, String osType, String controlPanel, int tier, int managedLevel, String shopperId);

    VirtualMachineCredit getVirtualMachineCredit(UUID orionGuid);

    UUID provisionVirtualMachine(UUID orionGuid, String name, long projectId, int specId, int managedLevel, long imageId);

    void addHfsVmIdToVirtualMachine(UUID vmId, long hfsVmId);
    
    void setHostname(UUID vmId, String hostname);
    
    void updateVirtualMachine(UUID vmId, Map<String, Object> paramsToUpdate);

    List<VirtualMachine> getVirtualMachinesForUser(long vps4UserId);

    List<VirtualMachineCredit> getVirtualMachineCredits(String shopperId);

    void createOrionRequestIfNoneExists(Vps4User vps4User);
    
    boolean virtualMachineHasCpanel(UUID vmId);

    VirtualMachineCredit getAndReserveCredit(UUID orionGuid);

}
