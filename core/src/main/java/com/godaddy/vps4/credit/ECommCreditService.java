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
import gdg.hfs.vhfs.ecomm.ECommService;
import gdg.hfs.vhfs.ecomm.MetadataUpdate;

@Singleton
public class ECommCreditService implements CreditService {

    private interface ProductMeta{
        String DATA_CENTER = "data_center";
        String PRODUCT_ID = "product_id";
    }

    private interface PlanFeatures{
        String TIER = "tier";
        String MANAGED_LEVEL = "managed_level";
        String MONITORING = "monitoring";
        String OPERATING_SYSTEM = "operatingsystem";
        String CONTROL_PANEL_TYPE = "control_panel_type";
        String PROVISION_DATE = "provision_date";
    }

    private static final Logger logger = LoggerFactory.getLogger(ECommCreditService.class);

    final ECommService ecommService;
    final DataCenterService dataCenterService;

    @Inject
    public  ECommCreditService(ECommService ecommService, DataCenterService dataCenterService) {
        this.ecommService = ecommService;
        this.dataCenterService = dataCenterService;
    }

    @Override
    public VirtualMachineCredit getVirtualMachineCredit(UUID orionGuid) {
        VirtualMachineCredit credit = null;
        try {
            Account account = ecommService.getAccount(orionGuid.toString());
            credit = mapVirtualMachineCredit(account);
        } catch(Exception ex) {
            logger.error("Error retrieving VPS4 credit for account guid {} : Exception :", orionGuid.toString(), ex);
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
                null, // create date was in credit table but not in ecomm account
                stringToInstant(account.product_meta.get(PlanFeatures.PROVISION_DATE)),
                account.shopper_id,
                AccountStatus.valueOf(account.status.name().toUpperCase()),
                getDataCenter(account), getProductId(account));
    }

    private UUID getProductId(Account account) {
        String productIdStr = account.product_meta.get(ProductMeta.PRODUCT_ID);
        UUID productId = null;
        if (productIdStr != null){
            productId = UUID.fromString(productIdStr);
        }
        return productId;
    }

    private DataCenter getDataCenter(Account account) {
        if(account.product_meta.containsKey(ProductMeta.DATA_CENTER)){
            int dcId = Integer.valueOf(account.product_meta.get(ProductMeta.DATA_CENTER));
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
        return getVirtualMachineCredits(shopperId, true);
    }

    @Override
    public List<VirtualMachineCredit> getVirtualMachineCredits(String shopperId) {
        return getVirtualMachineCredits(shopperId, false);
    }

    public List<VirtualMachineCredit> getVirtualMachineCredits(String shopperId, boolean showClaimed) {
        List<Account> accounts = ecommService.getAccounts(shopperId);
        Stream<Account> stream = accounts.stream()
                .filter(a -> a.product.equals("vps4"))
                .filter(a -> a.status != Account.Status.removed);

        if (showClaimed)
            stream = stream.filter(a -> !a.product_meta.containsKey(ProductMeta.DATA_CENTER));

        return stream.map(this::mapVirtualMachineCredit)
                .collect(Collectors.toList());
    }

    @Override
    public void createVirtualMachineCredit(UUID orionGuid, String operatingSystem, String controlPanel,
                                          int tier, int managedLevel, int monitoring, String shopperId) {
        Map<String, String> planFeatures = new HashMap<>();
        planFeatures.put(PlanFeatures.TIER, String.valueOf(tier));
        planFeatures.put(PlanFeatures.MANAGED_LEVEL, String.valueOf(managedLevel));
        planFeatures.put(PlanFeatures.MONITORING, String.valueOf(monitoring));
        planFeatures.put(PlanFeatures.OPERATING_SYSTEM, operatingSystem);
        planFeatures.put(PlanFeatures.CONTROL_PANEL_TYPE, controlPanel);

        Account account = new Account();
        account.shopper_id = shopperId;
        account.account_guid = orionGuid.toString();
        account.product = "vps4";
        account.status = Account.Status.active;
        account.plan_features = planFeatures;

        ecommService.createAccount(account);
    }

    @Override
    public void claimVirtualMachineCredit(UUID orionGuid, int dataCenterId, UUID productId) {
        Map<String,String> from = new HashMap<>();
        from.put(ProductMeta.DATA_CENTER, null);
        from.put(PlanFeatures.PROVISION_DATE, null);
        from.put(ProductMeta.PRODUCT_ID, null);

        Map<String,String> to = new HashMap<>();
        to.put(ProductMeta.DATA_CENTER, String.valueOf(dataCenterId));
        to.put(PlanFeatures.PROVISION_DATE, Instant.now().toString());
        to.put(ProductMeta.PRODUCT_ID, productId.toString());

        MetadataUpdate prodMeta = new MetadataUpdate();
        prodMeta.from = from;
        prodMeta.to = to;

        ecommService.updateProductMetadata(orionGuid.toString(), prodMeta);
    }

    @Override
    public void unclaimVirtualMachineCredit(UUID orionGuid) {
        Account account = ecommService.getAccount(orionGuid.toString());
        Map<String,String> from = new HashMap<>();
        from.put(ProductMeta.DATA_CENTER, account.product_meta.get(ProductMeta.DATA_CENTER));
        from.put(PlanFeatures.PROVISION_DATE, account.product_meta.get(PlanFeatures.PROVISION_DATE));
        from.put(ProductMeta.PRODUCT_ID, account.product_meta.get(ProductMeta.PRODUCT_ID));

        Map<String,String> to = new HashMap<>();
        to.put(ProductMeta.DATA_CENTER, null);
        to.put(PlanFeatures.PROVISION_DATE, null);
        to.put(ProductMeta.PRODUCT_ID, null);

        MetadataUpdate prodMeta = new MetadataUpdate();
        prodMeta.from = from;
        prodMeta.to = to;

        ecommService.updateProductMetadata(orionGuid.toString(), prodMeta);
    }
}
