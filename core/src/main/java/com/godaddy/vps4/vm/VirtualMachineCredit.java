package com.godaddy.vps4.vm;

import java.time.Instant;
import java.util.UUID;

public class VirtualMachineCredit {

    public final UUID orionGuid;
    public final int tier;
    public final int managedLevel;
    public final String operatingSystem;
    public final String controlPanel;
    public final Instant createDate;
    public final Instant provisionDate;
    public final String shopperId;
    public final int monitoring;

    public VirtualMachineCredit(UUID orionGuid, int tier, int managedLevel, String operatingSystem, String controlPanel, Instant createDate,
            Instant provisionDate, String shopperId, int monitoring) {
        this.orionGuid = orionGuid;
        this.tier = tier;
        this.managedLevel = managedLevel;
        this.operatingSystem = operatingSystem;
        this.controlPanel = controlPanel;
        this.createDate = createDate;
        this.provisionDate = provisionDate;
        this.shopperId = shopperId;
        this.monitoring = monitoring;
    }

    @Override
    public String toString() {
        return String.format(
                "VirtualMachineRequest [orionGuid: %s tier: %d managedLevel: %d operatingSystem: %s controlPanel: %s createDate: %s provisionDate: %s shopperId: %s monitoring: %d]",
                orionGuid, tier, managedLevel, operatingSystem, controlPanel, createDate, provisionDate, shopperId, monitoring);
    }
}
