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

    public String resellerId;
    public String shopperId;
    public String mssql;

    public ProdMeta prodMeta;
    public EntitlementData entitlementData;

    public VirtualMachineCredit() {
        prodMeta = new ProdMeta();
        entitlementData = new EntitlementData();
    }

    @JsonIgnore
    public boolean isAccountSuspended() {
        return entitlementData.accountStatus == AccountStatus.SUSPENDED || entitlementData.accountStatus == AccountStatus.ABUSE_SUSPENDED;
    }

    @JsonIgnore
    public boolean isAccountRemoved() {
        return entitlementData.accountStatus == AccountStatus.REMOVED;
    }

    @JsonIgnore
    public boolean isOwnedByShopper(String ssoShopperId) {
        return ssoShopperId.equals(shopperId);
    }

    @JsonIgnore
    public boolean isUsable() {
        return prodMeta.provisionDate == null;
    }

    @JsonProperty("hasMonitoring")
    public boolean hasMonitoring() {
        if (this.isDed4() && this.entitlementData.managedLevel == 1)
        {
            return false;
        }
        return entitlementData.monitoring == MONITORING_ENABLED;
    }

    public Instant getPurchasedAt() {
        return prodMeta.purchasedAt;
    }

    public boolean isManaged() {
        switch (entitlementData.managedLevel) {
            case 2:
                return true;
            case 1:
                if (!isDed4())
                    return true;
        }
        return false;
    }

    public boolean isDed4() {
        return entitlementData.tier >= 60;
    }

    @JsonIgnore
    public boolean isAccountActive() {
        return entitlementData.accountStatus == AccountStatus.ACTIVE;
    }

    @Override
    public String toString() {
        return ReflectionToStringBuilder.toString(this, ToStringStyle.SHORT_PREFIX_STYLE);
    }

    public UUID getEntitlementId() {
        return entitlementData.entitlementId;
    }

    public int getTier() {
        return entitlementData.tier;
    }

    public int getManagedLevel() {
        return entitlementData.managedLevel;
    }

    public String getOperatingSystem() {
        return entitlementData.operatingSystem;
    }

    public String getControlPanel() {
        return entitlementData.controlPanel;
    }

    public Instant getProvisionDate() {
        return prodMeta.provisionDate;
    }

    public String getShopperId() {
        return shopperId;
    }

    public int getMonitoring() {
        return entitlementData.monitoring;
    }

    public AccountStatus getAccountStatus() {
        return entitlementData.accountStatus;
    }

    public DataCenter getDataCenter() {
        return prodMeta.dataCenter;
    }

    public UUID getProductId() {
        return prodMeta.productId;
    }

    public boolean isFullyManagedEmailSent() {
        return prodMeta.fullyManagedEmailSent;
    }

    public String getResellerId() {
        return resellerId;
    }

    public boolean isPlanChangePending() {
        return prodMeta.planChangePending;
    }

    public int getPfid() {
        return entitlementData.pfid;
    }

    public Instant getExpireDate() { return entitlementData.expireDate; }

    public UUID getCustomerId() { return entitlementData.customerId; }

    public String getMssql() {
        return mssql;
    }

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
            credit.entitlementData.entitlementId = this.accountGuid;
            if (planFeatures != null) {
                credit.entitlementData.tier = Integer.parseInt(planFeatures.getOrDefault(PlanFeatures.TIER.toString(), "10"));
                credit.entitlementData.managedLevel =
                        Integer.parseInt(planFeatures.getOrDefault(PlanFeatures.MANAGED_LEVEL.toString(), "0"));
                credit.entitlementData.monitoring = parseMonitoring(planFeatures.getOrDefault(PlanFeatures.MONITORING.toString(), "0"));
                credit.entitlementData.operatingSystem = planFeatures.get(PlanFeatures.OPERATINGSYSTEM.toString());
                if (credit.entitlementData.operatingSystem == null) {
                    // Alternative field name from entitlement gateway
                    credit.entitlementData.operatingSystem = planFeatures.get("operating_system");
                }
                credit.entitlementData.controlPanel = planFeatures.get(PlanFeatures.CONTROL_PANEL_TYPE.toString());
                credit.entitlementData.pfid = Integer.parseInt(planFeatures.getOrDefault(PlanFeatures.PF_ID.toString(), "0"));
                credit.mssql = planFeatures.get(PlanFeatures.MSSQL.toString());
            }

            if (productMeta != null) {
                credit.prodMeta.provisionDate = getDateFromProductMeta(ProductMetaField.PROVISION_DATE.toString());
                credit.prodMeta.purchasedAt = getDateFromProductMeta(ProductMetaField.PURCHASED_AT.toString());
                credit.prodMeta.fullyManagedEmailSent = getFlagFromProductMeta(ProductMetaField.FULLY_MANAGED_EMAIL_SENT.toString());
                credit.prodMeta.planChangePending = getFlagFromProductMeta(ProductMetaField.PLAN_CHANGE_PENDING.toString());
                credit.prodMeta.dataCenter = getDataCenter();
                credit.prodMeta.productId = getProductId();
            }

            credit.shopperId = this.shopperId;
            credit.entitlementData.accountStatus = this.accountStatus;
            credit.resellerId = this.resellerId;
            credit.entitlementData.customerId = this.customerId;
            credit.entitlementData.expireDate = this.expireDate;

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
