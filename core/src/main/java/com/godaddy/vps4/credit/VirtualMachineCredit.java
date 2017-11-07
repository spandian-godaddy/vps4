package com.godaddy.vps4.credit;

import java.time.Instant;
import java.util.UUID;

import org.apache.commons.lang3.builder.ReflectionToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.godaddy.vps4.vm.AccountStatus;
import com.godaddy.vps4.vm.DataCenter;

public class VirtualMachineCredit {

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

    public VirtualMachineCredit(){
    }
    
    public VirtualMachineCredit(UUID orionGuid, int tier, int managedLevel, int monitoring, String operatingSystem, String controlPanel,
            Instant provisionDate, String shopperId, AccountStatus accountStatus, DataCenter dataCenter, UUID vmId,
            boolean fullyManagedEmailSent) {
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
