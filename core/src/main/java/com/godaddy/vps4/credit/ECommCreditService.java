package com.godaddy.vps4.credit;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.godaddy.vps4.security.Vps4User;
import com.godaddy.vps4.vm.AccountStatus;

import gdg.hfs.vhfs.ecomm.Account;
import gdg.hfs.vhfs.ecomm.ECommService;
import gdg.hfs.vhfs.ecomm.MetadataUpdate;


public class ECommCreditService implements CreditService {

    private static final Logger logger = LoggerFactory.getLogger(ECommCreditService.class);

    final ECommService ecommService;

    @Inject
    public  ECommCreditService(ECommService ecommService) {
        this.ecommService = ecommService;
    }

    @Override
    public VirtualMachineCredit getVirtualMachineCredit(UUID orionGuid) {
        VirtualMachineCredit credit = null;
        try {
            Account account = ecommService.getAccount(orionGuid.toString());
            credit = mapVirtualMachineCredit(account);
        } catch(Exception ex) {
            logger.error("Error retrieving VPS4 credit for account guid {}", orionGuid.toString());
        }

        return credit;
    }

    private VirtualMachineCredit mapVirtualMachineCredit(Account account) {
        return new VirtualMachineCredit(UUID.fromString(account.account_guid),
                Integer.parseInt(account.plan_features.get("tier")),
                Integer.parseInt(account.plan_features.get("managed_level")),
                Integer.parseInt(account.plan_features.get("monitoring")),
                account.plan_features.get("os"),
                account.plan_features.get("control_panel_type"),
                null, // create date was in credit table but not in ecomm account
                stringToInstant(account.product_meta.get("provision_date")),
                account.shopper_id,
                AccountStatus.valueOf(account.status.name().toUpperCase()));
    }

    private Instant stringToInstant(String provisionDate) {
        if (provisionDate != null)
            return Instant.parse(provisionDate);
        return null;
    }

    @Override
    public List<VirtualMachineCredit> getVirtualMachineCredits(String shopperId) {
        List<Account> accounts = ecommService.getAccounts(shopperId);
        return accounts.stream()
                .filter(a -> a.status == Account.Status.active)
                .filter(a -> !a.product_meta.containsKey("data_center"))
                .map(this::mapVirtualMachineCredit)
                .collect(Collectors.toList());
    }

    @Override
    public void createVirtualMachineCredit(UUID orionGuid, String operatingSystem, String controlPanel,
                                          int tier, int managedLevel, String shopperId) {
        Map<String, String> planFeatures = new HashMap<>();
        planFeatures.put("tier", String.valueOf(tier));
        planFeatures.put("managed_level", String.valueOf(managedLevel));
        planFeatures.put("operatingsystem", operatingSystem);
        planFeatures.put("control_panel_type", controlPanel);

        Account account = new Account();
        account.shopper_id = shopperId;
        account.account_guid = orionGuid.toString();
        account.product = "vps4";
        account.status = Account.Status.active;
        account.plan_features = planFeatures;

        ecommService.createAccount(account);
    }

    @Override
    public synchronized void createCreditIfNoneExists(Vps4User vps4User) {
        String shopperId = vps4User.getShopperId();
        if (getVirtualMachineCredits(shopperId).isEmpty())
            this.createVirtualMachineCredit(UUID.randomUUID(), "linux", "cpanel", 10, 1, shopperId);
    }

    @Override
    public void claimVirtualMachineCredit(UUID orionGuid, int dataCenterId) {
        Map<String,String> from = new HashMap<>();
        from.put("data_center", null);
        from.put("provision_date", null);

        Map<String,String> to = new HashMap<>();
        to.put("data_center", String.valueOf(dataCenterId));
        to.put("provision_date", Instant.now().toString());

        MetadataUpdate prodMeta = new MetadataUpdate();
        prodMeta.from = from;
        prodMeta.to = to;

        ecommService.updateProductMetadata(orionGuid.toString(), prodMeta);
    }

    @Override
    public void unclaimVirtualMachineCredit(UUID orionGuid) {
        Account account = ecommService.getAccount(orionGuid.toString());
        Map<String,String> from = new HashMap<>();
        from.put("data_center", account.product_meta.get("data_center"));
        from.put("provision_date", account.product_meta.get("provision_date"));

        Map<String,String> to = new HashMap<>();
        to.put("data_center", null);
        to.put("provision_date", null);

        MetadataUpdate prodMeta = new MetadataUpdate();
        prodMeta.from = from;
        prodMeta.to = to;

        ecommService.updateProductMetadata(orionGuid.toString(), prodMeta);
    }
}