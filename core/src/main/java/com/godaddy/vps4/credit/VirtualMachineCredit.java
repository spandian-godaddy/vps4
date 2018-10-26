package com.godaddy.vps4.credit;

import java.time.Instant;
import java.util.UUID;

import org.apache.commons.lang3.builder.ReflectionToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.godaddy.vps4.vm.AccountStatus;
import com.godaddy.vps4.vm.DataCenter;

public class VirtualMachineCredit {

    private final int FULLY_MANAGED_LEVEL = 2;
    private final int MONITORING_ENABLED = 1;

    public UUID orionGuid;
    public int tier;
    public int managedLevel;
    public String operatingSystem;
    public String controlPanel;
    public Instant provisionDate;
    public String shopperId;
    public int monitoring;
    public AccountStatus accountStatus;
    public DataCenter dataCenter;
    public UUID productId;
    public boolean fullyManagedEmailSent;
    public String resellerId;
    public boolean planChangePending;

    public VirtualMachineCredit(){
    }

    public VirtualMachineCredit(UUID orionGuid, int tier, int managedLevel, int monitoring, String operatingSystem,
            String controlPanel, Instant provisionDate, String shopperId, AccountStatus accountStatus,
            DataCenter dataCenter, UUID vmId, boolean fullyManagedEmailSent, String resellerId, boolean planChangePending) {
        this.orionGuid = orionGuid;
        this.tier = tier;
        this.managedLevel = managedLevel;
        this.monitoring = monitoring;
        this.operatingSystem = operatingSystem;
        this.controlPanel = controlPanel;
        this.provisionDate = provisionDate;
        this.shopperId = shopperId;
        this.accountStatus = accountStatus;
        this.dataCenter = dataCenter;
        this.productId = vmId;
        this.fullyManagedEmailSent = fullyManagedEmailSent;
        this.resellerId = resellerId;
        this.planChangePending = planChangePending;
    }

    @JsonIgnore
    public boolean isAccountSuspended() {
        return accountStatus == AccountStatus.SUSPENDED ||
                accountStatus == AccountStatus.ABUSE_SUSPENDED;
    }

    @JsonIgnore
    public boolean isAccountRemoved() {
        return accountStatus == AccountStatus.REMOVED;
    }

    @JsonIgnore
    public boolean isOwnedByShopper(String ssoShopperId) {
        return ssoShopperId.equals(shopperId);
    }

    @JsonIgnore
    public boolean isUsable() {
        return provisionDate == null;
    }

    @JsonIgnore
    public boolean hasMonitoring() {
        return monitoring == MONITORING_ENABLED || isFullyManaged();
    }

    @JsonIgnore
    public boolean isFullyManaged() {
        return managedLevel == FULLY_MANAGED_LEVEL;
    }

    @JsonIgnore
    public boolean isAccountActive() { return accountStatus == AccountStatus.ACTIVE; }

    @Override
    public String toString() {
        return ReflectionToStringBuilder.toString(this, ToStringStyle.SHORT_PREFIX_STYLE);
    }
}
