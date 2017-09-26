package com.godaddy.vps4.credit;

import java.time.Instant;
import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.godaddy.vps4.vm.AccountStatus;
import com.godaddy.vps4.vm.DataCenter;
import org.apache.commons.lang3.builder.ReflectionToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

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

    @JsonIgnore
    public boolean isAccountSuspended() {
        return accountStatus == AccountStatus.SUSPENDED ||
                accountStatus == AccountStatus.ABUSE_SUSPENDED;
    }

    @JsonIgnore
    public boolean isOwnedByShopper(String ssoShopperId) {
        return ssoShopperId.equals(shopperId);
    }

    @JsonIgnore
    public boolean isUsable() {
        return provisionDate == null;

    }

    @Override
    public String toString() {
        return ReflectionToStringBuilder.toString(this, ToStringStyle.MULTI_LINE_STYLE);
    }
}
