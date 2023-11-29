package com.godaddy.vps4.entitlement;


import com.godaddy.vps4.entitlement.models.*;

import java.util.UUID;

public interface EntitlementsService {
    Entitlement getEntitlement(UUID customerId, UUID entitlementId);
    Entitlement[] getEntitlements(UUID customerId);
    Entitlement updateCommonName(UUID customerId, UUID entitlementId, String commonName);
    Entitlement suspendEntitlement(UUID customerId, UUID entitlementId, String suspendReason);
    Entitlement reinstateEntitlement(UUID customerId, UUID entitlementId, String suspendReason);
    Entitlement provisionEntitlement(UUID customerId, UUID entitlementId, String managementConsoleUrl, String commonName,
                              String provisioningTracker, String result, String serviceStartDate, String serviceEndDate,
                              String firstAutomatedDate, String lastPossibleDate);
    Entitlement updateEntitlementConsole(UUID customerId, UUID entitlementId, String managementConsoleUrl);

}
