package com.godaddy.vps4.credit;

import java.time.Instant;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.inject.Inject;
import javax.ws.rs.WebApplicationException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.godaddy.vps4.vm.AccountStatus;
import com.godaddy.vps4.vm.DataCenter;
import com.godaddy.vps4.vm.DataCenterService;
import com.google.inject.Singleton;

import gdg.hfs.vhfs.ecomm.Account;
import gdg.hfs.vhfs.ecomm.ECommDataCache;
import gdg.hfs.vhfs.ecomm.ECommService;
import gdg.hfs.vhfs.ecomm.MetadataUpdate;

@Singleton
public class ECommCreditService implements CreditService {

    private static final String PRODUCT_NAME = "vps4";

    public enum ProductMetaField {
        DATA_CENTER,
        PRODUCT_ID,
        PROVISION_DATE,
        FULLY_MANAGED_EMAIL_SENT,
        PLAN_CHANGE_PENDING;

        @Override
        public String toString() {
            return name().toLowerCase();
        }
    }

    enum PlanFeatures {
        TIER,
        MANAGED_LEVEL,
        MONITORING,
        OPERATINGSYSTEM,
        CONTROL_PANEL_TYPE;

        @Override
        public String toString() {
            return name().toLowerCase();
        }
    }

    private static final Logger logger = LoggerFactory.getLogger(ECommCreditService.class);

    private final ECommService ecommService;
    private final DataCenterService dataCenterService;

    @Inject
    public  ECommCreditService(ECommService ecommService, DataCenterService dataCenterService) {
        this.ecommService = ecommService;
        this.dataCenterService = dataCenterService;
    }

    @Override
    public VirtualMachineCredit getVirtualMachineCredit(UUID orionGuid) {
        Account account = getHfsEcommAccount(orionGuid);
        if (account == null)
            return null;

        return mapVirtualMachineCredit(account);
    }

    private Account getHfsEcommAccount(UUID orionGuid) {
        try {
            Account account = ecommService.getAccount(orionGuid.toString());
            logger.info("Account: {}", account);
            return account;
        } catch(Exception ex) {
            logger.error("Error retrieving VPS4 credit for account guid {} : Exception :", orionGuid.toString(), ex);
        }
        return null; // return null since we can't find the credit. Keeps the semantics of this method consistent
    }

    private VirtualMachineCredit mapVirtualMachineCredit(Account account) {
        try {
            VirtualMachineCredit credit = new VirtualMachineCredit(UUID.fromString(account.account_guid),
                Integer.parseInt(account.plan_features.get(PlanFeatures.TIER.toString())),
                Integer.parseInt(account.plan_features.get(PlanFeatures.MANAGED_LEVEL.toString())),
                Integer.parseInt(account.plan_features.getOrDefault(PlanFeatures.MONITORING.toString(), "0")),
                account.plan_features.get(PlanFeatures.OPERATINGSYSTEM.toString()),
                account.plan_features.get(PlanFeatures.CONTROL_PANEL_TYPE.toString()),
                stringToInstant(account.product_meta.get(ProductMetaField.PROVISION_DATE.toString())),
                getShopperId(account),
                AccountStatus.valueOf(account.status.name().toUpperCase()),
                getDataCenter(account), getProductId(account),
                Boolean.parseBoolean(account.product_meta.get(ProductMetaField.FULLY_MANAGED_EMAIL_SENT.toString())),
                account.reseller_id,
                Boolean.parseBoolean(account.product_meta.get(ProductMetaField.PLAN_CHANGE_PENDING.toString())));
            logger.info("Credit: {}", credit.toString());
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
        // Brand resellers will use sub_account_shopper_id, otherwise use shopper_id
        return (account.sub_account_shopper_id != null) ? account.sub_account_shopper_id : account.shopper_id;
    }

    private UUID getProductId(Account account) {
        String productIdStr = account.product_meta.get(ProductMetaField.PRODUCT_ID.toString());
        UUID productId = null;
        if (productIdStr != null){
            productId = UUID.fromString(productIdStr);
        }
        return productId;
    }

    private DataCenter getDataCenter(Account account) {
        if (account.product_meta.containsKey(ProductMetaField.DATA_CENTER.toString())) {
            int dcId = Integer.valueOf(account.product_meta.get(ProductMetaField.DATA_CENTER.toString()));
            return this.dataCenterService.getDataCenter(dcId);
        }
        return null;
    }

    private Instant stringToInstant(String provisionDate) {
        if (provisionDate != null)
            return Instant.parse(provisionDate);
        return null;
    }

    @Override
    public List<VirtualMachineCredit> getUnclaimedVirtualMachineCredits(String shopperId) {
        return getVirtualMachineCredits(shopperId, false);
    }

    @Override
    public List<VirtualMachineCredit> getVirtualMachineCredits(String shopperId) {
        return getVirtualMachineCredits(shopperId, true);
    }

    private List<VirtualMachineCredit> getVirtualMachineCredits(String shopperId, boolean showClaimed) {
        List<Account> accounts = ecommService.getAccounts(shopperId);
        Stream<Account> stream = accounts.stream()
                .filter(a -> a.product.equals(PRODUCT_NAME))
                .filter(a -> a.status != Account.Status.removed);

        if (!showClaimed)
            stream = stream.filter(a -> !a.product_meta.containsKey(ProductMetaField.DATA_CENTER.toString()));

        return stream.map(this::mapVirtualMachineCredit)
                .collect(Collectors.toList());
    }

    @Override
    public void createVirtualMachineCredit(UUID orionGuid, String shopperId, String operatingSystem, String controlPanel,
                                          int tier, int managedLevel, int monitoring, int resellerId) {
        Map<PlanFeatures, String> planFeatures = new EnumMap<>(PlanFeatures.class);
        planFeatures.put(PlanFeatures.TIER, String.valueOf(tier));
        planFeatures.put(PlanFeatures.MANAGED_LEVEL, String.valueOf(managedLevel));
        planFeatures.put(PlanFeatures.MONITORING, String.valueOf(monitoring));
        planFeatures.put(PlanFeatures.OPERATINGSYSTEM, operatingSystem);
        planFeatures.put(PlanFeatures.CONTROL_PANEL_TYPE, controlPanel);

        Account account = new Account();
        account.shopper_id = shopperId;
        account.account_guid = orionGuid.toString();
        account.product = PRODUCT_NAME;
        account.status = Account.Status.active;
        account.plan_features = fromEnumMap(planFeatures);
        account.reseller_id = String.valueOf(resellerId);

        ecommService.createAccount(account);
    }

    @Override
    public void claimVirtualMachineCredit(UUID orionGuid, int dataCenterId, UUID productId) {
        Map<ProductMetaField, String> to = new EnumMap<>(ProductMetaField.class);

        to.put(ProductMetaField.DATA_CENTER, String.valueOf(dataCenterId));
        to.put(ProductMetaField.PROVISION_DATE, Instant.now().toString());
        to.put(ProductMetaField.PRODUCT_ID, productId.toString());

        updateProductMeta(orionGuid, to);
    }

    @Override
    public void unclaimVirtualMachineCredit(UUID orionGuid, UUID productId) {
        EnumMap<ProductMetaField, String> expectedFrom = new EnumMap<>(ProductMetaField.class);
        expectedFrom.put(ProductMetaField.PRODUCT_ID, productId.toString());

        EnumMap<ProductMetaField, String> requestedTo = new EnumMap<>(ProductMetaField.class);
        requestedTo.put(ProductMetaField.DATA_CENTER, null);
        requestedTo.put(ProductMetaField.PROVISION_DATE, null);
        requestedTo.put(ProductMetaField.PRODUCT_ID, null);

        try {
            updateProductMeta(orionGuid, requestedTo, expectedFrom);
        } catch (WebApplicationException ex) {
            logger.warn("Failed to update product meta : ", ex);
        }
    }

    @Override
    public Map<ProductMetaField, String> getProductMeta(UUID orionGuid) {
        return toEnumMap(ProductMetaField.class, getCurrentProductMeta(orionGuid));
    }

    private Map<String, String> getCurrentProductMeta(UUID orionGuid) {
        Account account = ecommService.getAccount(orionGuid.toString());
        return account.product_meta;
    }

    @Override
    public void setCommonName(UUID orionGuid, String newName){
        ECommDataCache edc = new ECommDataCache();
        edc.common_name = newName;
        ecommService.setCommonName(orionGuid.toString(), edc);
    }

    @Override
    public void updateProductMeta(UUID orionGuid, Map<ProductMetaField, String> requestedTo, Map<ProductMetaField, String> expectedFrom) {
        // initialize both to and from JSON objects to the current prodMeta of the ecomm credit
        MetadataUpdate metaUpdateReq = new MetadataUpdate();
        metaUpdateReq.to = getCurrentProductMeta(orionGuid);
        metaUpdateReq.from = new HashMap<>(metaUpdateReq.to);

        metaUpdateReq.to.putAll(fromEnumMap(requestedTo));
        metaUpdateReq.from.putAll(fromEnumMap(expectedFrom));

        ecommService.updateProductMetadata(orionGuid.toString(), metaUpdateReq);
    }

    private <K extends Enum<K>> Map<String, String> fromEnumMap(Map<K, String> enumMap) {
        Map<String, String> stringMap = new HashMap<>();
        enumMap.forEach((k,v) -> stringMap.put(k.toString(), v));
        return stringMap;
    }

    private <K extends Enum<K>> Map<K, String> toEnumMap(Class<K> clazz, Map<String, String> stringMap) {
        Map<K, String> enumMap = new EnumMap<>(clazz);
        stringMap.forEach((k,v) -> enumMap.put(K.valueOf(clazz, k.toUpperCase()), v));
        return enumMap;
    }

    @Override
    public void updateProductMeta(UUID orionGuid, Map<ProductMetaField, String> updates) {
        MetadataUpdate prodMeta = new MetadataUpdate();
        prodMeta.from = getCurrentProductMeta(orionGuid);
        prodMeta.to = new HashMap<>(prodMeta.from);
        prodMeta.to.putAll(fromEnumMap(updates));

        ecommService.updateProductMetadata(orionGuid.toString(), prodMeta);
    }

    @Override
    public void updateProductMeta(UUID orionGuid, ProductMetaField field, String value) {
        MetadataUpdate prodMeta = new MetadataUpdate();
        prodMeta.from = getCurrentProductMeta(orionGuid);
        prodMeta.to = new HashMap<>(prodMeta.from);

        prodMeta.to.put(field.toString(), value);
        logger.info("Updating product meta for credit : {}, product_meta : {}", orionGuid, prodMeta);

        ecommService.updateProductMetadata(orionGuid.toString(), prodMeta);
    }

    @Override
    public void setStatus(UUID orionGuid, AccountStatus accountStatus) {
        Account account = ecommService.getAccount(orionGuid.toString());

        account.status = getEcommAccountStatus(accountStatus);
        logger.info("Updating status for credit {} to {}", orionGuid, accountStatus.toString().toLowerCase());
        ecommService.updateAccount(orionGuid.toString(), account);
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
        throw new IllegalArgumentException("Account status " + accountStatus.toString()
                + " does not have a corresponding status in the ECommCreditService.");
    }
}
