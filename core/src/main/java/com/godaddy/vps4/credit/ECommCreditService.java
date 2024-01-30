package com.godaddy.vps4.credit;

import com.godaddy.vps4.vm.AccountStatus;
import com.godaddy.vps4.vm.DataCenterService;
import com.google.inject.Singleton;
import gdg.hfs.vhfs.ecomm.Account;
import gdg.hfs.vhfs.ecomm.ECommDataCache;
import gdg.hfs.vhfs.ecomm.ECommService;
import gdg.hfs.vhfs.ecomm.MetadataUpdate;
import gdg.hfs.vhfs.ecomm.Reinstatement;
import gdg.hfs.vhfs.ecomm.SuspendReason;
import gdg.hfs.vhfs.ecomm.Suspension;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;
import java.time.Instant;
import java.util.Collections;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Singleton
public class ECommCreditService implements CreditService {

    private static final String PRODUCT_NAME = "vps4";

    public enum ProductMetaField {
        DATA_CENTER,
        PRODUCT_ID,
        PROVISION_DATE,
        FULLY_MANAGED_EMAIL_SENT,
        PLAN_CHANGE_PENDING,
        PURCHASED_AT,
        RELEASED_AT,
        RELAY_COUNT,
        SUSPENDED,
        BILLING_SUSPENDED_FLAG;

        @Override
        public String toString() {
            return name().toLowerCase();
        }
    }

    public enum PlanFeatures {
        TIER,
        MANAGED_LEVEL,
        MONITORING,
        OPERATINGSYSTEM,
        CONTROL_PANEL_TYPE,
        PF_ID,
        MSSQL,
        CDNWAF;

        @Override
        public String toString() {
            return name().toLowerCase();
        }
    }

    public enum SuspensionReason {
        FRAUD,
        POLICY,
        LEGAL
    }

    private static final Logger logger = LoggerFactory.getLogger(ECommCreditService.class);

    private final ECommService ecommService;
    private final DataCenterService dataCenterService;

    @Inject
    public ECommCreditService(ECommService ecommService, DataCenterService dataCenterService) {
        this.ecommService = ecommService;
        this.dataCenterService = dataCenterService;
    }

    @Override
    public VirtualMachineCredit getVirtualMachineCredit(UUID entitlementId) {
        Account account = getHfsEcommAccount(entitlementId);
        if (account == null) {
            return null;
        }
        
        return mapVirtualMachineCredit(account);
    }

    private Account getHfsEcommAccount(UUID entitlementId) {
        try {
            Account account = ecommService.getAccount(entitlementId.toString());
            logger.info("Account: {}", account);
            return account;
        } catch (Exception ex) {
            logger.error("Error retrieving VPS4 credit for account guid {} : Exception :", entitlementId, ex);
        }
        return null; // return null since we can't find the credit. Keeps the semantics of this method consistent
    }

    private VirtualMachineCredit mapVirtualMachineCredit(Account account) {
        try {
            VirtualMachineCredit credit = new VirtualMachineCredit.Builder(dataCenterService)
                    .withPlanFeatures(account.plan_features)
                    .withProductMeta(account.product_meta)
                    .withAccountGuid(account.account_guid)
                    .withAccountStatus(AccountStatus.valueOf(account.status.name().toUpperCase()))
                    .withResellerID(account.reseller_id)
                    .withShopperID(getShopperId(account))
                    .withCustomerID(account.customer_id)
                    .withExpireDate(account.expire_date)
                    .build();
            logger.info("Credit: {}", credit);
            return credit;
        } catch (Exception ex) {
            logger.error("Error mapping VPS4 credit for account guid {} : Exception :", account.account_guid, ex);
            if (ex.getMessage().startsWith("Sql.")) {
                // If this is a SQL exception then re-throw exception
                throw ex;
            }
        }

        return null;
    }

    private String getShopperId(Account account) {
        return account.shopper_id;
    }

    @Override
    public List<VirtualMachineCredit> getVirtualMachineCredits(String shopperId, boolean showClaimed) {
        List<Account> accounts = ecommService.getAccounts(shopperId);
        Stream<Account> stream = accounts.stream()
                .filter(a -> a.product.equals(PRODUCT_NAME))
                .filter(a -> a.status != Account.Status.removed);

        if (!showClaimed) {
            stream = stream.filter(a -> !a.product_meta.containsKey(ProductMetaField.DATA_CENTER.toString()));
        }

        return stream.filter(Objects::nonNull).map(this::mapVirtualMachineCredit).collect(Collectors.toList());
    }

    @Override
    public void createVirtualMachineCredit(UUID entitlementId, String shopperId, String operatingSystem,
                                           String controlPanel,
                                           int tier, int managedLevel, int monitoring, int resellerId) {
        Map<PlanFeatures, String> planFeatures = new EnumMap<>(PlanFeatures.class);
        planFeatures.put(PlanFeatures.TIER, String.valueOf(tier));
        planFeatures.put(PlanFeatures.MANAGED_LEVEL, String.valueOf(managedLevel));
        planFeatures.put(PlanFeatures.MONITORING, String.valueOf(monitoring));
        planFeatures.put(PlanFeatures.OPERATINGSYSTEM, operatingSystem);
        planFeatures.put(PlanFeatures.CONTROL_PANEL_TYPE, controlPanel);

        Account account = new Account();
        account.shopper_id = shopperId;
        account.account_guid = entitlementId.toString();
        account.product = PRODUCT_NAME;
        account.status = Account.Status.active;
        account.plan_features = fromEnumMap(planFeatures);
        account.reseller_id = String.valueOf(resellerId);

        ecommService.createAccount(account);
    }

    @Override
    public void claimVirtualMachineCredit(UUID entitlementId, int dataCenterId, UUID productId) {
        Map<ProductMetaField, String> to = new EnumMap<>(ProductMetaField.class);

        to.put(ProductMetaField.DATA_CENTER, String.valueOf(dataCenterId));
        to.put(ProductMetaField.PROVISION_DATE, Instant.now().toString());
        to.put(ProductMetaField.PRODUCT_ID, productId.toString());
        to.put(ProductMetaField.RELAY_COUNT, null);
        to.put(ProductMetaField.RELEASED_AT, null);

        updateProductMeta(entitlementId, to);
    }

    @Override
    public void unclaimVirtualMachineCredit(UUID entitlementId, UUID productId, int currentMailRelays) {
        EnumMap<ProductMetaField, String> expectedFrom = new EnumMap<>(ProductMetaField.class);
        expectedFrom.put(ProductMetaField.PRODUCT_ID, productId.toString());

        EnumMap<ProductMetaField, String> requestedTo = new EnumMap<>(ProductMetaField.class);
        requestedTo.put(ProductMetaField.DATA_CENTER, null);
        requestedTo.put(ProductMetaField.PROVISION_DATE, null);
        requestedTo.put(ProductMetaField.PRODUCT_ID, null);
        if(currentMailRelays > 0) {
            requestedTo.put(ProductMetaField.RELEASED_AT, Instant.now().toString());
            requestedTo.put(ProductMetaField.RELAY_COUNT, String.valueOf(currentMailRelays));
        }

        try {
            updateProductMeta(entitlementId, requestedTo, expectedFrom);
        } catch (WebApplicationException ex) {
            logger.warn("Failed to update product meta : ", ex);
        }
    }

    @Override
    public Map<ProductMetaField, String> getProductMeta(UUID entitlementId) {
        return toEnumMap(ProductMetaField.class, getCurrentProductMeta(entitlementId));
    }

    private Map<String, String> getCurrentProductMeta(UUID entitlementId) {
        Account account = ecommService.getAccount(entitlementId.toString());
        // Important to add unset enum keys with null value in map or update calls to ecomm vertical will fail
        Map<String, String> mapWithNullVals = new HashMap<>(account.product_meta);
        Stream.of(ProductMetaField.values())
                .map(ProductMetaField::toString)
                .forEach(field -> mapWithNullVals.put(field, account.product_meta.get(field)));
        return mapWithNullVals;
    }

    @Override
    public void setCommonName(UUID entitlementId, String newName) {
        ECommDataCache edc = new ECommDataCache();
        edc.common_name = newName;
        ecommService.setCommonName(entitlementId.toString(), edc);
    }

    @Override
    public void updateProductMeta(UUID entitlementId, ProductMetaField field, String value) {
        updateProductMeta(entitlementId, Collections.singletonMap(field, value));
    }

    @Override
    public void updateProductMeta(UUID entitlementId, Map<ProductMetaField, String> updates) {
        updateProductMeta(entitlementId, updates, Collections.emptyMap());
    }

    @Override
    public void updateProductMeta(UUID entitlementId, Map<ProductMetaField, String> requestedTo,
                                  Map<ProductMetaField, String> expectedFrom) {
        // initialize both to and from JSON objects to the current prodMeta of the ecomm credit
        MetadataUpdate prodMeta = new MetadataUpdate();
        prodMeta.to = getCurrentProductMeta(entitlementId);
        prodMeta.from = new HashMap<>(prodMeta.to);

        prodMeta.to.putAll(fromEnumMap(requestedTo));
        prodMeta.from.putAll(fromEnumMap(expectedFrom));

        prodMeta.to.replaceAll((k,v) -> cleanProdMeta(k,v));

        ecommService.updateProductMetadata(entitlementId.toString(), prodMeta);
    }

    private String cleanProdMeta(String key, String value) {
        // Set value of non-existing enum fields and boolean false flags to null
        // This will cause hfs to remove the fields and keep the ecomm prod meta data clean
        if (value == null || value.equals("false"))
            return null;

        try {
            ProductMetaField.valueOf(key.toUpperCase());
        } catch (IllegalArgumentException ex) {
            return null;
        }

        return value;
    }

    private <K extends Enum<K>> Map<String, String> fromEnumMap(Map<K, String> enumMap) {
        Map<String, String> stringMap = new HashMap<>();
        enumMap.forEach((k, v) -> stringMap.put(k.toString(), v));
        return stringMap;
    }

    private <K extends Enum<K>> Map<K, String> toEnumMap(Class<K> clazz, Map<String, String> stringMap) {
        Map<K, String> enumMap = new EnumMap<>(clazz);
        stringMap.forEach((k, v) -> enumMap.put(K.valueOf(clazz, k.toUpperCase()), v));
        return enumMap;
    }

    @Override
    public void setStatus(UUID entitlementId, AccountStatus accountStatus) {
        Account account = ecommService.getAccount(entitlementId.toString());

        account.status = getEcommAccountStatus(accountStatus);
        logger.info("Updating status for credit {} to {}", entitlementId, accountStatus.toString().toLowerCase());
        ecommService.updateAccount(entitlementId.toString(), account);
    }

    private Account.Status getEcommAccountStatus(AccountStatus accountStatus) {
        switch (accountStatus) {
            case ACTIVE:
                return Account.Status.active;
            case SUSPENDED:
                return Account.Status.suspended;
            case ABUSE_SUSPENDED:
                return Account.Status.abuse_suspended;
            case REMOVED:
                return Account.Status.removed;
        }
        throw new IllegalArgumentException("Account status " + accountStatus
                + " does not have a corresponding status in the ECommCreditService.");
    }

    @Override
    public void submitSuspend(UUID entitlementId, SuspensionReason reason) throws Exception {
        Suspension suspension = new Suspension();
        suspension.suspendReason = convertVps4ReasonToHfsReason(reason);
        try {
            Response response = ecommService.suspend(entitlementId.toString(), null, suspension);
            if(response.getStatus() != 204) throw new Exception(String.format("Failed to suspend %s with", entitlementId));
        } catch (Exception ex) {
            logger.error("Error suspending VPS4 credit for account guid {} : Exception :", entitlementId, ex);
            throw ex;
        }
    }

    private SuspendReason convertVps4ReasonToHfsReason(SuspensionReason reason) {
        switch (reason){
            case FRAUD : return SuspendReason.FRAUD;
            case LEGAL: return SuspendReason.LEGAL;
            case POLICY: return SuspendReason.POLICY;
        }
        return null;
    }

    @Override
    public void submitReinstate(UUID entitlementId, SuspensionReason reason) throws Exception {
        Reinstatement reinstatement = new Reinstatement();
        reinstatement.suspendReason = convertVps4ReasonToHfsReason(reason);
        try {
            Response response = ecommService.reinstate(entitlementId.toString(), null, reinstatement);
            if(response.getStatus() != 204) throw new Exception(String.format("Failed to reinstate %s with", entitlementId));
        } catch (Exception ex) {
            logger.error("Error reinstating VPS4 credit for account guid {} : Exception :", entitlementId, ex);
            throw ex;
        }
    }
}
