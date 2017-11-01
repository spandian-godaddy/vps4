package com.godaddy.vps4.vm;

import java.time.Instant;
import java.util.UUID;

import com.godaddy.vps4.network.IpAddress;

public class VirtualMachine {

    public UUID vmId;
    public long hfsVmId;
    public UUID orionGuid;
    public long projectId;
    public VirtualMachineSpec spec;
    public String name;
    public Image image;
    public IpAddress primaryIpAddress;
    public Instant validOn;
    public Instant validUntil;
    public String hostname;
    public int managedLevel;
    public UUID backupJobId;
	
    public VirtualMachine() {
    }

    public VirtualMachine(UUID vmId,
            long hfsVmId,
            UUID orionGuid,
            long projectId,
            VirtualMachineSpec spec,
            String name,
            Image image,
            IpAddress primaryIpAddress,
            Instant validOn,
            Instant validUntil,
            String hostname,
            int managedLevel,
            UUID backupJobId) {
        this.vmId = vmId;
        this.hfsVmId = hfsVmId;
        this.orionGuid = orionGuid;
        this.projectId = projectId;
        this.spec = spec;
        this.name = name;
        this.image = image;
        this.primaryIpAddress = primaryIpAddress;
        this.validOn = validOn;
        this.validUntil = validUntil;
        this.hostname = hostname;
        this.managedLevel = managedLevel;
        this.backupJobId = backupJobId;
    }

    public VirtualMachine(VirtualMachine virtualMachine) {
        vmId = virtualMachine.vmId;
        hfsVmId = virtualMachine.hfsVmId;
        orionGuid = virtualMachine.orionGuid;
        projectId = virtualMachine.projectId;
        spec = virtualMachine.spec;
        name = virtualMachine.name;
        image = virtualMachine.image;
        primaryIpAddress = virtualMachine.primaryIpAddress;
        validOn = virtualMachine.validOn;
        validUntil = virtualMachine.validUntil;
        hostname = virtualMachine.hostname;
        managedLevel = virtualMachine.managedLevel;
        backupJobId = virtualMachine.backupJobId;
    }

    @Override
    public String toString() {
        return String.format(
                "VirtualMachine [vmId=%s, hfsVmId=%d, orionGuid=%s, projectId=%d, spec=%s, name=%s, hostname=%s, image=%s, primaryIpAddress=%s, managedLevel=%d, validOn=%s, validUntil=%s]",
                vmId, hfsVmId, orionGuid, projectId, spec.name, name, hostname, image == null ? "" : image.imageName,
                primaryIpAddress == null ? "" : primaryIpAddress.ipAddress, managedLevel, validOn, validUntil);
    }
}