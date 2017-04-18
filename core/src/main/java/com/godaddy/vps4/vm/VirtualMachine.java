package com.godaddy.vps4.vm;

import java.time.Instant;
import java.util.UUID;

import com.godaddy.vps4.network.IpAddress;

public class VirtualMachine {

    public final UUID vmId;
    public final long hfsVmId;
    public final UUID orionGuid;
    public final long projectId;
    public final VirtualMachineSpec spec;
    public final String name;
    public final Image image;
    public final IpAddress primaryIpAddress;
    public final DataCenter dataCenter;
    public final Instant validOn;
    public final Instant validUntil;
    public final String hostname;
    public final AccountStatus accountStatus;

    public VirtualMachine(UUID id,
            long vmId,
            UUID orionGuid,
            long projectId,
            VirtualMachineSpec spec,
            String name,
            Image image,
            IpAddress primaryIpAddress,
            DataCenter dataCenter,
            Instant validOn,
            Instant validUntil,
            String hostname,
            AccountStatus accountStatus) {
        this.vmId = id;
        this.hfsVmId = vmId;
        this.orionGuid = orionGuid;
        this.projectId = projectId;
        this.spec = spec;
        this.name = name;
        this.image = image;
        this.primaryIpAddress = primaryIpAddress;
        this.dataCenter = dataCenter;
        this.validOn = validOn;
        this.validUntil = validUntil;
        this.hostname = hostname;
        this.accountStatus = accountStatus;
    }

    public VirtualMachine(VirtualMachine virtualMachine) {
        this.vmId = virtualMachine.vmId;
        this.hfsVmId = virtualMachine.hfsVmId;
        this.orionGuid = virtualMachine.orionGuid;
        this.projectId = virtualMachine.projectId;
        this.spec = virtualMachine.spec;
        this.name = virtualMachine.name;
        this.image = virtualMachine.image;
        this.primaryIpAddress = virtualMachine.primaryIpAddress;
        this.dataCenter = virtualMachine.dataCenter;
        this.validOn = virtualMachine.validOn;
        this.validUntil = virtualMachine.validUntil;
        this.hostname = virtualMachine.hostname;
        this.accountStatus = virtualMachine.accountStatus;
    }

    @Override
    public String toString() {
        return String.format(
                "VirtualMachine [vmId=%s, hfsVmId=%d, orionGuid=%s, projectId=%d, spec=%s, name=%s, hostname=%s, image=%s, primaryIpAddress=%s, dataCenter=%s, accountStatus=%s, validOn=%s, validUntil=%s]",
                vmId, hfsVmId, orionGuid, projectId, spec.name, name, hostname, image == null ? "" : image.imageName,
                primaryIpAddress == null ? "" : primaryIpAddress.ipAddress, dataCenter == null ? "" : dataCenter.dataCenterName,
                accountStatus.toString(), validOn, validUntil);
    }

}
