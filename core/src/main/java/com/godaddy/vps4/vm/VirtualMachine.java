package com.godaddy.vps4.vm;

import java.time.Instant;
import java.util.UUID;

import org.apache.commons.lang3.builder.ReflectionToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.godaddy.vps4.network.IpAddress;

public class VirtualMachine {

    public UUID vmId;
    public long hfsVmId;
    public UUID orionGuid;
    public long projectId;
    public ServerSpec spec;
    public String name;
    public Image image;
    public IpAddress primaryIpAddress;
    public Instant validOn;
    public Instant canceled;
    public Instant validUntil;
    public Instant nydusWarningAck;
    public String hostname;
    public int managedLevel;
    public UUID backupJobId;
    public DataCenter dataCenter;
    public String currentOs;

    public VirtualMachine() {
    }

    public VirtualMachine(UUID vmId,
            long hfsVmId,
            UUID orionGuid,
            long projectId,
            ServerSpec spec,
            String name,
            Image image,
            IpAddress primaryIpAddress,
            Instant validOn,
            Instant canceled,
            Instant validUntil,
            Instant nydusWarningAck,
            String hostname,
            int managedLevel,
            UUID backupJobId,
            DataCenter dataCenter,
            String currentOs) {
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
        this.nydusWarningAck = nydusWarningAck;
        this.hostname = hostname;
        this.managedLevel = managedLevel;
        this.backupJobId = backupJobId;
        this.dataCenter = dataCenter;
        this.currentOs = currentOs;
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
        nydusWarningAck = virtualMachine.nydusWarningAck;
        hostname = virtualMachine.hostname;
        managedLevel = virtualMachine.managedLevel;
        backupJobId = virtualMachine.backupJobId;
        dataCenter = virtualMachine.dataCenter;
        currentOs = virtualMachine.currentOs;
    }

    @JsonIgnore
    public boolean isActive() {
        return validUntil.isAfter(Instant.now());
    }

    @JsonIgnore
    public boolean isCanceledOrDeleted() {
        return canceled.isBefore(Instant.now()) || validUntil.isBefore(Instant.now());
    }

    @JsonIgnore
    public boolean hasNydusWarningAcked () {
        return nydusWarningAck.isBefore(Instant.now());
    }

    @Override
    public String toString() {
        return ReflectionToStringBuilder.toString(this, ToStringStyle.SHORT_PREFIX_STYLE);
    }
}
