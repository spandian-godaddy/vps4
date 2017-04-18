package com.godaddy.vps4.credit;

import java.time.Instant;
import java.util.UUID;

import com.godaddy.vps4.vm.AccountStatus;

public class VirtualMachineCredit {

    public final UUID orionGuid;
    public final int tier;
    public final int managedLevel;
    public final String operatingSystem;
    public final String controlPanel;
    public final Instant createDate;
    public final Instant provisionDate;
    public final String shopperId;
    public final AccountStatus accountStatus;

    public VirtualMachineCredit(UUID orionGuid, int tier, int managedLevel, String operatingSystem, String controlPanel, Instant createDate,
            Instant provisionDate, String shopperId, AccountStatus accountStatus) {
        this.orionGuid = orionGuid;
        this.tier = tier;
        this.managedLevel = managedLevel;
        this.operatingSystem = operatingSystem;
        this.controlPanel = controlPanel;
        this.createDate = createDate;
        this.provisionDate = provisionDate;
        this.shopperId = shopperId;
        this.accountStatus = accountStatus;
    }

    @Override
    public String toString() {
        return "VirtualMachineRequest [orionGuid: " + orionGuid + " tier: " + tier + " managedLevel: " + managedLevel +
                " operatingSystem: " + operatingSystem + " controlPanel: " + controlPanel + " createDate: " + createDate +
                " provisionDate: " + provisionDate + " shopperId: " + shopperId + " accountStatus: " + accountStatus + "]";
    }

}
