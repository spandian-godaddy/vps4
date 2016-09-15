package com.godaddy.vps4.vm;

import java.util.UUID;

public class VirtualMachine {

    public final long vmId;
    public final UUID orionGuid;
    public final long projectId;
    public final VirtualMachineSpec spec;
    public final String name;
    public final int controlPanelId;
    public final int osTypeId;

    public VirtualMachine(long vmId, UUID orionGuid, long projectId, VirtualMachineSpec spec, String name, int controlPanelId, int osTypeId) {
        this.vmId = vmId;
        this.orionGuid = orionGuid;
        this.projectId = projectId;
        this.spec = spec;
        this.name = name;
        this.controlPanelId = controlPanelId;
        this.osTypeId = osTypeId;
    }

    @Override
    public String toString() {
        return "VirtualMachine [vmId=" + vmId + ", projectId=" + projectId + ", spec="
                + spec.name + ", name=" + name + "]";
    }

}
