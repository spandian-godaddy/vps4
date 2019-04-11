package com.godaddy.vps4.credit;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

import com.godaddy.vps4.vm.DataCenterService;
import gdg.hfs.vhfs.ecomm.Account;
import org.apache.commons.lang3.builder.ReflectionToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.godaddy.vps4.vm.AccountStatus;
import com.godaddy.vps4.vm.DataCenter;

import com.godaddy.vps4.credit.ECommCreditService.PlanFeatures;
import com.godaddy.vps4.credit.ECommCreditService.ProductMetaField;

public class VirtualMachineCredit {

    private final int FULLY_MANAGED_LEVEL = 2;
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

    private VirtualMachineCredit(){
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

    public static class Builder {
        private Map<String, String> planFeatures;
        private Map<String, String> productMeta;
        private String shopperId;
        private UUID accountGuid;
        private String resellerId;
        private AccountStatus accountStatus;
        private final DataCenterService dataCenterService;

        public Builder(DataCenterService dataCenterService) {
            this.dataCenterService = dataCenterService;
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

        public Builder withAccountStatus(Account.Status accountStatus) {
            this.accountStatus = AccountStatus.valueOf(accountStatus.name().toUpperCase());
            return this;
        }

        private DataCenter getDataCenter() {
            DataCenter dc = null;
            if (productMeta.containsKey(ProductMetaField.DATA_CENTER.toString())) {
                int dcId = Integer.valueOf(productMeta.get(ProductMetaField.DATA_CENTER.toString()));
                dc = dataCenterService.getDataCenter(dcId);
            }
            return dc;
        }

        private UUID getProductId() {
            return productMeta.containsKey(ProductMetaField.PRODUCT_ID.toString())
                ? UUID.fromString(productMeta.get(ProductMetaField.PRODUCT_ID.toString()))
                : null;
        }

        public VirtualMachineCredit build() {
            VirtualMachineCredit credit = new VirtualMachineCredit();
            credit.orionGuid = this.accountGuid;
            if (planFeatures != null) {
                credit.tier = Integer.parseInt(planFeatures.getOrDefault(PlanFeatures.TIER.toString(), "10"));
                credit.managedLevel = Integer.parseInt(planFeatures.getOrDefault(PlanFeatures.MANAGED_LEVEL.toString(), "0"));
                credit.monitoring = Integer.parseInt(planFeatures.getOrDefault(PlanFeatures.MONITORING.toString(), "0"));
                credit.operatingSystem = planFeatures.get(PlanFeatures.OPERATINGSYSTEM.toString());
                credit.controlPanel = planFeatures.get(PlanFeatures.CONTROL_PANEL_TYPE.toString());
                credit.pfid = Integer.parseInt(planFeatures.getOrDefault(PlanFeatures.PF_ID.toString(), "0"));
            }

            if (productMeta != null) {
                String provisionDate = productMeta.get(ProductMetaField.PROVISION_DATE.toString());
                credit.provisionDate = (provisionDate != null) ? Instant.parse(provisionDate) : null;
                credit.fullyManagedEmailSent = Boolean.parseBoolean(
                        productMeta.get(ProductMetaField.FULLY_MANAGED_EMAIL_SENT.toString()));
                credit.planChangePending = Boolean.parseBoolean(
                        productMeta.get(ProductMetaField.PLAN_CHANGE_PENDING.toString()));
                credit.dataCenter = getDataCenter();
                credit.productId = getProductId();
            }

            credit.shopperId = this.shopperId;
            credit.accountStatus = this.accountStatus;
            credit.resellerId = this.resellerId;

            return credit;
        }
    }
}
