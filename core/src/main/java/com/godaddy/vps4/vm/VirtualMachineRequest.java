package com.godaddy.vps4.vm;

import java.time.Instant;
import java.util.UUID;

public class VirtualMachineRequest {

    public final UUID orionGuid;
    public final int tier;
    public final int managedLevel;
    public final String operatingSystem;
    public final String controlPanel;
    public final Instant createDate;
    public final Instant provisionDate;
    public final String shopperId;

    public VirtualMachineRequest(UUID orionGuid, int tier, int managedLevel, String operatingSystem, String controlPanel, Instant createDate, Instant provisionDate, String shopperId) {
        this.orionGuid = orionGuid;
        this.tier = tier;
        this.managedLevel = managedLevel;
        this.operatingSystem = operatingSystem;
        this.controlPanel = controlPanel;
        this.createDate = createDate;
        this.provisionDate = provisionDate;
        this.shopperId = shopperId;
    }

    @Override
    public String toString() {
        return "VirtualMachineRequest [orionGuid: " + orionGuid + " tier: " + tier + " managedLevel: " + managedLevel +
                " operatingSystem: " + operatingSystem + " controlPanel: " + controlPanel + " createDate: " + createDate +
                " provisionDate: " + provisionDate + " shopperId: " + shopperId + "]";
    }

}
