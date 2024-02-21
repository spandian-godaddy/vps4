package com.godaddy.vps4.credit;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import com.godaddy.vps4.credit.ECommCreditService.ProductMetaField;
import com.godaddy.vps4.vm.AccountStatus;

public interface CreditService {

    void createVirtualMachineCredit(UUID orionGuid, String shopperId, String osType, String controlPanel,
            int tier, int managedLevel, int monitoring, int resellerId);

    VirtualMachineCredit getVirtualMachineCredit(UUID orionGuid);

    VirtualMachineCredit getVpsCredit(UUID customerId, UUID entitlementId);

    List<VirtualMachineCredit> getVirtualMachineCredits(String shopperId, boolean showClaimed);

    void claimVirtualMachineCredit(UUID orionGuid, int dataCenterId, UUID productId);

    void unclaimVirtualMachineCredit(UUID orionGuid, UUID productId, int currentMailRelays);

    void setCommonName(UUID orionGuid, String newName);

    void updateProductMeta(UUID orionGuid, Map<ProductMetaField, String> updates);

    void updateProductMeta(UUID orionGuid, Map<ProductMetaField, String> requestedTo, Map<ProductMetaField, String> expectedFrom);

    void updateProductMeta(UUID orionGuid, ProductMetaField field, String value);

    Map<ProductMetaField, String> getProductMeta(UUID orionGuid);

    void setStatus(UUID orionGuid, AccountStatus accountStatus);

    void submitSuspend(UUID orionGuid, ECommCreditService.SuspensionReason reason) throws Exception;

    void submitReinstate(UUID orionGuid, ECommCreditService.SuspensionReason reason) throws Exception;
}