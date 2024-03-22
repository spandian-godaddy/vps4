package com.godaddy.vps4.credit;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.godaddy.vps4.credit.ECommCreditService.PlanFeatures;
import com.godaddy.vps4.credit.ECommCreditService.ProductMetaField;
import com.godaddy.vps4.entitlement.models.Product;
import com.godaddy.vps4.prodMeta.model.ProdMeta;
import com.godaddy.vps4.vm.AccountStatus;
import org.apache.commons.lang3.builder.ReflectionToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.TimeZone;
import java.time.Instant;
import java.util.Arrays;
import java.util.Date;
import java.util.Map;
import java.util.UUID;

public class VirtualMachineCredit {

    private final int MONITORING_ENABLED = 1;

    public String resellerId;
    public String shopperId;

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

    public UUID getProductId() {
        return prodMeta.productId;
    }

    public boolean isFullyManagedEmailSent() {
        return prodMeta.fullyManagedEmailSent;
    }

    public String getResellerId() {
        return resellerId;
    }

    public int getPfid() {
        return entitlementData.pfid;
    }

    public Instant getExpireDate() { return entitlementData.expireDate; }

    public UUID getCustomerId() { return entitlementData.customerId; }

    public String getMssql() {
        return entitlementData.mssql;
    }

    public int getCdnWaf() {
        return entitlementData.cdnWaf;
    }

    public static class Builder {
        private Map<String, String> planFeatures;
        private Map<String, String> productMeta;
        private String shopperId;
        private UUID accountGuid;
        private String resellerId;
        private AccountStatus accountStatus;
        private UUID customerId;
        private Instant expireDate;

        public Builder() {
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
                credit.entitlementData.mssql = planFeatures.get(PlanFeatures.MSSQL.toString());
                credit.entitlementData.cdnWaf = Integer.parseInt(planFeatures.getOrDefault(PlanFeatures.CDNWAF.toString(), "0"));
            }

            if (productMeta != null) {
                credit.prodMeta.provisionDate = getDateFromProductMeta(ProductMetaField.PROVISION_DATE.toString());
                credit.prodMeta.purchasedAt = getDateFromProductMeta(ProductMetaField.PURCHASED_AT.toString());
                credit.prodMeta.fullyManagedEmailSent = getFlagFromProductMeta(ProductMetaField.FULLY_MANAGED_EMAIL_SENT.toString());
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

        private int getDataCenter() {
            return productMeta.containsKey(ProductMetaField.DATA_CENTER.toString()) ?
                    Integer.parseInt(productMeta.get(ProductMetaField.DATA_CENTER.toString())) : 0;
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

    public static class EntitlementBuilder {
        private Product entitlementProduct;
        private Map<String, String> productMeta;
        private String shopperId;
        private UUID accountGuid;
        private String resellerId;
        private AccountStatus accountStatus;
        private UUID customerId;
        private Instant expireDate;

        public EntitlementBuilder() {
        }

        public VirtualMachineCredit build() {
            VirtualMachineCredit credit = new VirtualMachineCredit();
            credit.entitlementData.entitlementId = this.accountGuid;

            if (entitlementProduct != null) {
                credit.entitlementData.tier = entitlementProduct.planTier;
                credit.entitlementData.managedLevel = entitlementProduct.managedLevel;
                credit.entitlementData.monitoring = entitlementProduct.monitoring ? 1 : 0;
                credit.entitlementData.operatingSystem = entitlementProduct.operatingSystem;
                credit.entitlementData.controlPanel = entitlementProduct.controlPanelType;
                credit.entitlementData.mssql = entitlementProduct.mssql;
                credit.entitlementData.cdnWaf = entitlementProduct.cdnWaf != null ? entitlementProduct.cdnWaf : 0;
            }

            if (productMeta != null) {
                credit.prodMeta.provisionDate = getDateFromProductMeta(ProductMetaField.PROVISION_DATE.toString());
                credit.prodMeta.purchasedAt = getDateFromProductMeta(ProductMetaField.PURCHASED_AT.toString());
                credit.prodMeta.fullyManagedEmailSent = getFlagFromProductMeta(ProductMetaField.FULLY_MANAGED_EMAIL_SENT.toString());
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

        public EntitlementBuilder withEntitlementId(UUID entitlementId) {
            this.accountGuid = entitlementId;
            return this;
        }

        public EntitlementBuilder withEntitlementProduct(Product entitlementProduct) {
            this.entitlementProduct = entitlementProduct;
            return this;
        }

        public EntitlementBuilder withProductMeta(Map<String, String> productMeta) {
            this.productMeta = productMeta;
            return this;
        }

        public EntitlementBuilder withResellerID(String resellerId) {
            this.resellerId = resellerId;
            return this;
        }

        public EntitlementBuilder withShopperID(String shopperId) {
            this.shopperId = shopperId;
            return this;
        }

        public EntitlementBuilder withAccountStatus(String accountStatus) {
            this.accountStatus = AccountStatus.valueOf(accountStatus.toUpperCase());
            return this;
        }

        public EntitlementBuilder withCustomerID(UUID customerId) {
            this.customerId = customerId;
            return this;
        }

        private Instant parseStringDate(String date) throws ParseException {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
            sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
            return sdf.parse(date).toInstant();
        }

        public EntitlementBuilder withExpireDate(String expireDate) throws ParseException {
            this.expireDate = parseStringDate(expireDate);
            return this;
        }

        private int getDataCenter() {
            return productMeta.containsKey(ProductMetaField.DATA_CENTER.toString()) ?
                    Integer.parseInt(productMeta.get(ProductMetaField.DATA_CENTER.toString())) : 0;
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
    }
}
