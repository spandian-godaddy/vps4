package com.godaddy.vps4.credit;

import java.time.Instant;
import java.util.UUID;

import com.godaddy.vps4.vm.AccountStatus;
import com.godaddy.vps4.vm.DataCenter;

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
    public final AccountStatus accountStatus;
    public final DataCenter dataCenter;
    public final UUID productId;

    public VirtualMachineCredit(UUID orionGuid, int tier, int managedLevel, int monitoring, String operatingSystem,
            String controlPanel, Instant createDate, Instant provisionDate, String shopperId,
            AccountStatus accountStatus, DataCenter dataCenter, UUID vmId) {
        this.orionGuid = orionGuid;
        this.tier = tier;
        this.managedLevel = managedLevel;
        this.monitoring = monitoring;
        this.operatingSystem = operatingSystem;
        this.controlPanel = controlPanel;
        this.createDate = createDate;
        this.provisionDate = provisionDate;
        this.shopperId = shopperId;
        this.accountStatus = accountStatus;
        this.dataCenter = dataCenter;
        this.productId = vmId;
    }

    @Override
    public String toString() {
        return "VirtualMachineRequest [orionGuid: " + orionGuid + " tier: " + tier +
                " managedLevel: " + managedLevel + " monitoring: " + monitoring +
                " operatingSystem: " + operatingSystem + " controlPanel: " + controlPanel +
                " createDate: " + createDate + " provisionDate: " + provisionDate +
                " shopperId: " + shopperId + " accountStatus: " + accountStatus +
                " dataCenter: " + dataCenter.toString() + " productId: " + productId +"]";
    }
}
