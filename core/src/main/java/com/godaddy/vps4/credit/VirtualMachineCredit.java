package com.godaddy.vps4.credit;

import java.time.Instant;
import java.util.Arrays;
import java.util.Map;
import java.util.UUID;

import org.apache.commons.lang3.builder.ReflectionToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.godaddy.vps4.credit.ECommCreditService.PlanFeatures;
import com.godaddy.vps4.credit.ECommCreditService.ProductMetaField;
import com.godaddy.vps4.vm.AccountStatus;
import com.godaddy.vps4.vm.DataCenter;
import com.godaddy.vps4.vm.DataCenterService;

public class VirtualMachineCredit {
    @JsonIgnore
    private static final Instant MANAGED_LEVEL_V2_CUTOVER_DATE = Instant.parse("2099-05-01T00:00:00Z");
    private static final String MYH_CONTROL_PANEL = "MYH";

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
    private Instant purchasedAt;
    private boolean abuseSuspendedFlag;
    private boolean billingSuspendedFlag;
    private boolean panoptaInstalled;

    private VirtualMachineCredit() {
    }

    public enum EffectiveManagedLevel {
        SELF_MANAGED_V1,
        SELF_MANAGED_V2,
        MANAGED_V1,
        MANAGED_V2,
        FULLY_MANAGED
    }


    @JsonIgnore
    public boolean isAccountSuspended() {
        return accountStatus == AccountStatus.SUSPENDED ||
                accountStatus == AccountStatus.ABUSE_SUSPENDED;
    }

    @JsonIgnore
    public boolean isAccountAbuseSuspended() {
        return accountStatus == AccountStatus.ABUSE_SUSPENDED;
    }

    @JsonIgnore
    public boolean isAccountBillingSuspended() {
        return accountStatus == AccountStatus.SUSPENDED;
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
    public boolean isEffectivelySelfManaged() {
        return Arrays.asList(EffectiveManagedLevel.SELF_MANAGED_V1, EffectiveManagedLevel.SELF_MANAGED_V2)
                     .contains(this.effectiveManagedLevel());
    }

    @JsonIgnore
    public Instant getPurchasedAt() {
        return purchasedAt;
    }

    private boolean purchasedBeforeMLV2CutoffDate() {
        return (purchasedAt == null || purchasedAt.isBefore(MANAGED_LEVEL_V2_CUTOVER_DATE));
    }

    @JsonProperty("effectiveManagedLevel")
    public EffectiveManagedLevel effectiveManagedLevel() {
        EffectiveManagedLevel effectiveManagedLevel;
        switch (managedLevel) {
            case 2:
                effectiveManagedLevel = EffectiveManagedLevel.FULLY_MANAGED;
                break;
            case 1:
                effectiveManagedLevel = EffectiveManagedLevel.MANAGED_V2;
                break;
            default:
                if (purchasedBeforeMLV2CutoffDate()) {
                    // determine level based on control panel
                    if (controlPanel == null || MYH_CONTROL_PANEL.equalsIgnoreCase(controlPanel)) {
                        effectiveManagedLevel = EffectiveManagedLevel.SELF_MANAGED_V1;
                    } else {
                        effectiveManagedLevel = EffectiveManagedLevel.MANAGED_V1;
                    }
                } else {
                    effectiveManagedLevel = EffectiveManagedLevel.SELF_MANAGED_V2;
                }
                break;
        }
        return effectiveManagedLevel;

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

    public boolean isAbuseSuspendedFlagSet() {
        return abuseSuspendedFlag;
    }

    public boolean isBillingSuspendedFlagSet() {
        return billingSuspendedFlag;
    }

    public boolean isPanoptaInstalled() {
        return panoptaInstalled;
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

        public Builder withAccountStatus(AccountStatus accountStatus) {
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

        private Instant getDateFromProductMeta(String metaFieldName) {
            String date = productMeta.get(metaFieldName);
            return (date != null) ? Instant.parse(date) : null;
        }

        private boolean getFlagFromProductMeta(String productMetaFieldName) {
            return productMeta.containsKey(productMetaFieldName) &&
                    Boolean.parseBoolean(productMeta.get(productMetaFieldName));
        }

        public VirtualMachineCredit build() {
            VirtualMachineCredit credit = new VirtualMachineCredit();
            credit.orionGuid = this.accountGuid;
            if (planFeatures != null) {
                credit.tier = Integer.parseInt(planFeatures.getOrDefault(PlanFeatures.TIER.toString(), "10"));
                credit.managedLevel =
                        Integer.parseInt(planFeatures.getOrDefault(PlanFeatures.MANAGED_LEVEL.toString(), "0"));
                credit.monitoring =
                        Integer.parseInt(planFeatures.getOrDefault(PlanFeatures.MONITORING.toString(), "0"));
                credit.operatingSystem = planFeatures.get(PlanFeatures.OPERATINGSYSTEM.toString());
                credit.controlPanel = planFeatures.get(PlanFeatures.CONTROL_PANEL_TYPE.toString());
                credit.pfid = Integer.parseInt(planFeatures.getOrDefault(PlanFeatures.PF_ID.toString(), "0"));
            }

            if (productMeta != null) {
                credit.provisionDate = getDateFromProductMeta(ProductMetaField.PROVISION_DATE.toString());
                credit.purchasedAt = getDateFromProductMeta(ProductMetaField.PURCHASED_AT.toString());
                credit.fullyManagedEmailSent = Boolean.parseBoolean(
                        productMeta.get(ProductMetaField.FULLY_MANAGED_EMAIL_SENT.toString()));
                credit.planChangePending = Boolean.parseBoolean(
                        productMeta.get(ProductMetaField.PLAN_CHANGE_PENDING.toString()));
                credit.dataCenter = getDataCenter();
                credit.productId = getProductId();
                credit.abuseSuspendedFlag = getFlagFromProductMeta(ProductMetaField.ABUSE_SUSPENDED_FLAG.toString());
                credit.billingSuspendedFlag =
                        getFlagFromProductMeta(ProductMetaField.BILLING_SUSPENDED_FLAG.toString());
                credit.panoptaInstalled = getFlagFromProductMeta(ProductMetaField.PANOPTA_INSTALLED.toString());
            }

            credit.shopperId = this.shopperId;
            credit.accountStatus = this.accountStatus;
            credit.resellerId = this.resellerId;

            return credit;
        }
    }

}
