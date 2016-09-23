package com.godaddy.vps4.vm;

import java.time.Instant;
import java.util.UUID;

public class VirtualMachine {

    public final long vmId;
    public final UUID orionGuid;
    public final long projectId;
    public final VirtualMachineSpec spec;
    public final String name;
    public final String image;
    public final Instant validOn;
    public final Instant validUntil;

    public VirtualMachine(long vmId,
            UUID orionGuid,
            long projectId,
            VirtualMachineSpec spec,
            String name,
            String image,
            Instant validOn,
            Instant validUntil) {
        this.vmId = vmId;
        this.orionGuid = orionGuid;
        this.projectId = projectId;
        this.spec = spec;
        this.name = name;
        this.image = image;
        this.validOn = validOn;
        this.validUntil = validUntil;
    }

    @Override
    public String toString() {
        return "VirtualMachine [vmId=" + vmId + ", orionGuid= " + orionGuid + ", projectId=" + projectId + ", spec="
                + spec.name + ", name=" + name + ", image=" + image + ", validOn=" + validOn + ", validUntil=" + validUntil + "]";
    }

}
