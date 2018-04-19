package com.godaddy.vps4.vm;

import java.time.Instant;
import java.util.UUID;
import org.apache.commons.lang3.builder.ReflectionToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import com.godaddy.vps4.network.IpAddress;

public class VirtualMachine {


    public static final int FULLY_MANAGED_LEVEL = 2;

    public UUID vmId;
    public long hfsVmId;
    public UUID orionGuid;
    public long projectId;
    public VirtualMachineSpec spec;
    public String name;
    public Image image;
    public IpAddress primaryIpAddress;
    public Instant validOn;
    public Instant canceled;
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
            Instant canceled,
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
        this.canceled = canceled;
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
        canceled = virtualMachine.canceled;
        validUntil = virtualMachine.validUntil;
        hostname = virtualMachine.hostname;
        managedLevel = virtualMachine.managedLevel;
        backupJobId = virtualMachine.backupJobId;
    }

    @Override
    public String toString() {
        return ReflectionToStringBuilder.toString(this, ToStringStyle.MULTI_LINE_STYLE);
    }

    public boolean isFullyManaged() {
        return this.managedLevel == FULLY_MANAGED_LEVEL;
    }
}
