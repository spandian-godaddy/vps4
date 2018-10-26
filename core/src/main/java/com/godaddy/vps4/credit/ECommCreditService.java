package com.godaddy.vps4.credit;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.inject.Inject;

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
        DATA_CENTER, PRODUCT_ID, PROVISION_DATE, FULLY_MANAGED_EMAIL_SENT, PLAN_CHANGE_PENDING;

        @Override
        public String toString() {
            return name().toLowerCase();
        }
    }

    private interface PlanFeatures{
        String TIER = "tier";
        String MANAGED_LEVEL = "managed_level";
        String MONITORING = "monitoring";
        String OPERATING_SYSTEM = "operatingsystem";
        String CONTROL_PANEL_TYPE = "control_panel_type";
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
        VirtualMachineCredit credit = null;
        Account account = null;
        try {
            account = ecommService.getAccount(orionGuid.toString());
            logger.info("Account: {}", account);
        } catch(Exception ex) {
            logger.error("Error retrieving VPS4 credit for account guid {} : Exception :", orionGuid.toString(), ex);
            return null; // return null since we can't find the credit. Keeps the semantics of this method consistent
        }

        if (account != null) {
            try {
                credit = mapVirtualMachineCredit(account);
                logger.info("Credit: {}", credit.toString());
            } catch (RuntimeException ex) {
                logger.error("Error mapping VPS4 credit for account guid {} : Exception :", orionGuid.toString(), ex);
                if (ex.getMessage().startsWith("Sql.")) {
                    // If this is a SQL exception then re-throw exception
                    throw ex;
                }
            } catch (Exception ex) {
                logger.error("Error mapping VPS4 credit for account guid {} : Exception :", orionGuid.toString(), ex);
            }
        }

        return credit;
    }

    private VirtualMachineCredit mapVirtualMachineCredit(Account account) {
        return new VirtualMachineCredit(UUID.fromString(account.account_guid),
                Integer.parseInt(account.plan_features.get(PlanFeatures.TIER)),
                Integer.parseInt(account.plan_features.get(PlanFeatures.MANAGED_LEVEL)),
                Integer.parseInt(account.plan_features.getOrDefault(PlanFeatures.MONITORING, "0")),
                account.plan_features.get(PlanFeatures.OPERATING_SYSTEM),
                account.plan_features.get(PlanFeatures.CONTROL_PANEL_TYPE),
                stringToInstant(account.product_meta.get(ProductMetaField.PROVISION_DATE.toString())),
                getShopperId(account),
                AccountStatus.valueOf(account.status.name().toUpperCase()),
                getDataCenter(account), getProductId(account),
                Boolean.parseBoolean(account.product_meta.get(ProductMetaField.FULLY_MANAGED_EMAIL_SENT.toString())),
                account.reseller_id,
                Boolean.parseBoolean(account.product_meta.get(ProductMetaField.PLAN_CHANGE_PENDING.toString())));
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
        Map<String, String> planFeatures = new HashMap<>();
        planFeatures.put(PlanFeatures.TIER, String.valueOf(tier));
        planFeatures.put(PlanFeatures.MANAGED_LEVEL, String.valueOf(managedLevel));
        planFeatures.put(PlanFeatures.MONITORING, String.valueOf(monitoring));
        planFeatures.put(PlanFeatures.OPERATING_SYSTEM, operatingSystem);
        planFeatures.put(PlanFeatures.CONTROL_PANEL_TYPE, controlPanel);

        Account account = new Account();
        account.shopper_id = shopperId;
        account.account_guid = orionGuid.toString();
        account.product = PRODUCT_NAME;
        account.status = Account.Status.active;
        account.plan_features = planFeatures;
        account.reseller_id = String.valueOf(resellerId);

        ecommService.createAccount(account);
    }

    @Override
    public void claimVirtualMachineCredit(UUID orionGuid, int dataCenterId, UUID productId) {
        Map<ProductMetaField, String> to = new HashMap<>();

        to.put(ProductMetaField.DATA_CENTER, String.valueOf(dataCenterId));
        to.put(ProductMetaField.PROVISION_DATE, Instant.now().toString());
        to.put(ProductMetaField.PRODUCT_ID, productId.toString());

        updateProductMeta(orionGuid, to);
    }

    @Override
    public void unclaimVirtualMachineCredit(UUID orionGuid) {
        Map<ProductMetaField, String> to = new HashMap<>();

        to.put(ProductMetaField.DATA_CENTER, null);
        to.put(ProductMetaField.PROVISION_DATE, null);
        to.put(ProductMetaField.PRODUCT_ID, null);

        updateProductMeta(orionGuid, to);
    }

    @Override
    public Map<ProductMetaField, String> getProductMeta(UUID orionGuid) {
        Map<String, String> currentProductMeta = getCurrentProductMeta(orionGuid);
        Map<ProductMetaField, String> productMeta = new HashMap<>();

        for (String field : currentProductMeta.keySet()) {
            productMeta.put(ProductMetaField.valueOf(field.toUpperCase()), currentProductMeta.get(field));
        }


        return productMeta;
    }

    private Map<String, String> getCurrentProductMeta(UUID orionGuid) {
        Account account = ecommService.getAccount(orionGuid.toString());
        Map<String,String> from = new HashMap<>();
        Stream.of(ProductMetaField.values()).forEach(field -> from.put(field.toString(), account.product_meta.get(field.toString())));
        return from;
    }

    @Override
    public void setCommonName(UUID orionGuid, String newName){
        ECommDataCache edc = new ECommDataCache();
        edc.common_name = newName;
        ecommService.setCommonName(orionGuid.toString(), edc);
    }

    @Override
    public void updateProductMeta(UUID orionGuid, Map<ProductMetaField, String> updates) {
        MetadataUpdate prodMeta = new MetadataUpdate();
        prodMeta.from = getCurrentProductMeta(orionGuid);
        prodMeta.to = new HashMap<>(prodMeta.from);

        for (Map.Entry<ProductMetaField, String> update : updates.entrySet()) {
            prodMeta.to.put(update.getKey().toString(), update.getValue());
        }

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
