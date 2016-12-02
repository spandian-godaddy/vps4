package com.godaddy.vps4.vm;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import com.godaddy.vps4.security.Vps4User;

public interface VirtualMachineService {

    List<VirtualMachine> getVirtualMachinesForProject(long projectId);

    VirtualMachine getVirtualMachine(long vmId);

    VirtualMachine getVirtualMachine(UUID orionGuid);

    VirtualMachineSpec getSpec(String name);

    VirtualMachineSpec getSpec(int tier);

    /**
     * @param vmId - the VM ID from the VM Vertical Service
     * @param projectId
     * @param spec
     * @param name
     * @return
     */
    VirtualMachine createVirtualMachine(long vmId, long projectId, String spec, String name);

    // updateStatus(long vmId, int newStatus)

    void destroyVirtualMachine(long vmId); // (just updates status/sets validUntil, destroy is accomplished on backend)

    void createVirtualMachineRequest(UUID orionGuid, String osType, String controlPanel, int tier, int managedLevel, String shopperId);

    VirtualMachineRequest getVirtualMachineRequest(UUID orionGuid);

    void provisionVirtualMachine(long vmId, UUID orionGuid, String name, long projectId, int specId, int managedLevel, long imageId);

    void updateVirtualMachine(long vmId, Map<String, Object> paramsToUpdate);

    List<VirtualMachine> getVirtualMachinesForUser(long vps4UserId);

    List<VirtualMachineRequest> getOrionRequests(String shopperId);

    void createOrionRequestIfNoneExists(Vps4User vps4User);

}
