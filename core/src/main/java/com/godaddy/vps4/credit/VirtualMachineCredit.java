package com.godaddy.vps4.credit;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.godaddy.vps4.credit.ECommCreditService.PlanFeatures;
import com.godaddy.vps4.credit.ECommCreditService.ProductMetaField;
import com.godaddy.vps4.vm.AccountStatus;
import com.godaddy.vps4.vm.DataCenter;
import com.godaddy.vps4.vm.DataCenterService;
import org.apache.commons.lang3.builder.ReflectionToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

import java.time.Instant;
import java.util.Arrays;
import java.util.Date;
import java.util.Map;
import java.util.UUID;

public class VirtualMachineCredit {

    private final int MONITORING_ENABLED = 1;

    private UUID orionGuid;
    private int tier;
    private int managedLevel;
    private String operatingSystem;
    private String controlPanel;
    private Instant provisionDate;
    private String shopperId;
    private int monitoring;
    private AccountStatus accountStatus;
    private DataCenter dataCenter;
    private UUID productId;
    private boolean fullyManagedEmailSent;
    private String resellerId;
    private boolean planChangePending;
    private int pfid;
    private Instant purchasedAt;
    private boolean suspended;
    private UUID customerId;
    private Instant expireDate;
    private boolean autoRenew;

    private VirtualMachineCredit() {
    }

    @JsonIgnore
    public boolean isAccountSuspended() {
        return accountStatus == AccountStatus.SUSPENDED || accountStatus == AccountStatus.ABUSE_SUSPENDED;
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

    @JsonProperty("hasMonitoring")
    public boolean hasMonitoring() {
        if (this.isDed4() && this.managedLevel == 1)
        {
            return false;
        }
        return monitoring == MONITORING_ENABLED;
    }

    public Instant getPurchasedAt() {
        return purchasedAt;
    }

    public boolean isManaged() {
        switch (managedLevel) {
            case 2:
                return true;
            case 1:
                if (!isDed4())
                    return true;
        }
        return false;
    }

    public boolean isDed4() {
        return tier >= 60;
    }

    @JsonIgnore
    public boolean isAccountActive() {
        return accountStatus == AccountStatus.ACTIVE;
    }

    @Override
    public String toString() {
        return ReflectionToStringBuilder.toString(this, ToStringStyle.SHORT_PREFIX_STYLE);
    }

    public UUID getOrionGuid() {
        return orionGuid;
    }

    public int getTier() {
        return tier;
    }

    public int getManagedLevel() {
        return managedLevel;
    }

    public String getOperatingSystem() {
        return operatingSystem;
    }

    public String getControlPanel() {
        return controlPanel;
    }

    public Instant getProvisionDate() {
        return provisionDate;
    }

    public String getShopperId() {
        return shopperId;
    }

    public int getMonitoring() {
        return monitoring;
    }

    public AccountStatus getAccountStatus() {
        return accountStatus;
    }

    public DataCenter getDataCenter() {
        return dataCenter;
    }

    public UUID getProductId() {
        return productId;
    }

    public boolean isFullyManagedEmailSent() {
        return fullyManagedEmailSent;
    }

    public String getResellerId() {
        return resellerId;
    }

    public boolean isPlanChangePending() {
        return planChangePending;
    }

    public int getPfid() {
        return pfid;
    }

    public boolean isVmSuspended() {
        return suspended;
    }

    public Instant getExpireDate() { return expireDate; }

    public boolean isAutoRenew() { return autoRenew; }

    public UUID getCustomerId() { return customerId; }

    public static class Builder {
        private Map<String, String> planFeatures;
        private Map<String, String> productMeta;
        private String shopperId;
        private UUID accountGuid;
        private String resellerId;
        private AccountStatus accountStatus;
        private final DataCenterService dataCenterService;
        private UUID customerId;
        private Instant expireDate;
        private boolean autoRenew;

        public Builder(DataCenterService dataCenterService) {
            this.dataCenterService = dataCenterService;
        }

        public VirtualMachineCredit build() {
            VirtualMachineCredit credit = new VirtualMachineCredit();
            credit.orionGuid = this.accountGuid;
            if (planFeatures != null) {
                credit.tier = Integer.parseInt(planFeatures.getOrDefault(PlanFeatures.TIER.toString(), "10"));
                credit.managedLevel =
                        Integer.parseInt(planFeatures.getOrDefault(PlanFeatures.MANAGED_LEVEL.toString(), "0"));
                credit.monitoring = parseMonitoring(planFeatures.getOrDefault(PlanFeatures.MONITORING.toString(), "0"));
                credit.operatingSystem = planFeatures.get(PlanFeatures.OPERATINGSYSTEM.toString());
                if (credit.operatingSystem == null) {
                    // Alternative field name from entitlement gateway
                    credit.operatingSystem = planFeatures.get("operating_system");
                }
                credit.controlPanel = planFeatures.get(PlanFeatures.CONTROL_PANEL_TYPE.toString());
                credit.pfid = Integer.parseInt(planFeatures.getOrDefault(PlanFeatures.PF_ID.toString(), "0"));
            }

            if (productMeta != null) {
                credit.provisionDate = getDateFromProductMeta(ProductMetaField.PROVISION_DATE.toString());
                credit.purchasedAt = getDateFromProductMeta(ProductMetaField.PURCHASED_AT.toString());
                credit.fullyManagedEmailSent = getFlagFromProductMeta(ProductMetaField.FULLY_MANAGED_EMAIL_SENT.toString());
                credit.planChangePending = getFlagFromProductMeta(ProductMetaField.PLAN_CHANGE_PENDING.toString());
                credit.dataCenter = getDataCenter();
                credit.productId = getProductId();
                credit.suspended = getFlagFromProductMeta(ProductMetaField.SUSPENDED.toString());
            }

            credit.shopperId = this.shopperId;
            credit.accountStatus = this.accountStatus;
            credit.resellerId = this.resellerId;
            credit.customerId = this.customerId;
            credit.expireDate = this.expireDate;
            credit.autoRenew = this.autoRenew;

            return credit;
        }

        public Builder withAccountGuid(String accountGuid) {
            this.accountGuid = UUID.fromString(accountGuid);
            return this;
        }

        public Builder withShopperID(String shopperId) {
            this.shopperId = shopperId;
            return this;
        }

        public Builder withPlanFeatures(Map<String, String> planFeatures) {
            this.planFeatures = planFeatures;
            return this;
        }

        public Builder withProductMeta(Map<String, String> productMeta) {
            this.productMeta = productMeta;
            return this;
        }

        public Builder withResellerID(String resellerId) {
            this.resellerId = resellerId;
            return this;
        }

        public Builder withAccountStatus(AccountStatus accountStatus) {
            this.accountStatus = AccountStatus.valueOf(accountStatus.name().toUpperCase());
            return this;
        }

        public Builder withCustomerID(String customerId) {
            this.customerId = customerId != null ? UUID.fromString(customerId) : null;
            return this;
        }

        public Builder withExpireDate(Date expireDate) {
            this.expireDate = expireDate != null ? expireDate.toInstant() : null;
            return this;
        }

        public Builder withAutoRenew(String autoRenew) {
            this.autoRenew = Boolean.parseBoolean(autoRenew);
            return this;
        }

        private DataCenter getDataCenter() {
            DataCenter dc = null;
            if (productMeta.containsKey(ProductMetaField.DATA_CENTER.toString())) {
                int dcId = Integer.parseInt(productMeta.get(ProductMetaField.DATA_CENTER.toString()));
                dc = dataCenterService.getDataCenter(dcId);
            }
            return dc;
        }

        private UUID getProductId() {
            return productMeta.containsKey(ProductMetaField.PRODUCT_ID.toString())
                    ? UUID.fromString(productMeta.get(ProductMetaField.PRODUCT_ID.toString()))
                    : null;
        }

        private Instant getDateFromProductMeta(String metaFieldName) {
            String date = productMeta.get(metaFieldName);
            return (date != null) ? Instant.parse(date) : null;
        }

        private boolean getFlagFromProductMeta(String productMetaFieldName) {
            return productMeta.containsKey(productMetaFieldName) &&
                    Boolean.parseBoolean(productMeta.get(productMetaFieldName));
        }

        private int parseMonitoring(String value) {
            return (Arrays.asList("true","1").contains(value)) ? 1 : 0;
        }
    }
}
