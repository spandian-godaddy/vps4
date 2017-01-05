package com.godaddy.vps4.vm;

import java.time.Instant;
import java.util.UUID;

import com.godaddy.vps4.network.IpAddress;

public class VirtualMachine {

    public final UUID id;
    public final long vmId;
    public final UUID orionGuid;
    public final long projectId;
    public final VirtualMachineSpec spec;
    public final String name;
    public final Image image;
    public final IpAddress primaryIpAddress;
    public final Instant validOn;
    public final Instant validUntil;

    public VirtualMachine(UUID id,
            long vmId,
            UUID orionGuid,
            long projectId,
            VirtualMachineSpec spec,
            String name,
            Image image,
            IpAddress primaryIpAddress,
            Instant validOn,
            Instant validUntil) {
        this.id = id;
        this.vmId = vmId;
        this.orionGuid = orionGuid;
        this.projectId = projectId;
        this.spec = spec;
        this.name = name;
        this.image = image;
        this.primaryIpAddress = primaryIpAddress;
        this.validOn = validOn;
        this.validUntil = validUntil;
    }

    public VirtualMachine(VirtualMachine virtualMachine) {
        this.id = virtualMachine.id;
        this.vmId = virtualMachine.vmId;
        this.orionGuid = virtualMachine.orionGuid;
        this.projectId = virtualMachine.projectId;
        this.spec = virtualMachine.spec;
        this.name = virtualMachine.name;
        this.image = virtualMachine.image;
        this.primaryIpAddress = virtualMachine.primaryIpAddress;
        this.validOn = virtualMachine.validOn;
        this.validUntil = virtualMachine.validUntil;
    }

    @Override
    public String toString() {
        return "VirtualMachine [id=" + id + ", vmId=" + vmId + ", orionGuid= " + orionGuid + ", projectId=" + projectId + ", spec="
                + spec.name + ", name=" + name + ", image=" + image.imageName + ", primaryIpAddress=" + primaryIpAddress.ipAddress
                + ", validOn=" + validOn + ", validUntil=" + validUntil + "]";
    }

}
