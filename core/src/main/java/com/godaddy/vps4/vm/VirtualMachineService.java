package com.godaddy.vps4.vm;

import java.util.List;

public interface VirtualMachineService {

    List<VirtualMachine> listVirtualMachines(long projectId);

    VirtualMachine getVirtualMachine(long vmId);

    VirtualMachineSpec getSpec(String name);

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

    void destroyVirtualMachine(long vmId);  // (just updates status/sets validUntil, destroy is accomplished on backend)


}
