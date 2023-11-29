package com.godaddy.vps4.entitlement;

import com.godaddy.vps4.entitlement.models.*;

import javax.inject.Inject;
import java.util.UUID;

public class DefaultEntitlementsService implements EntitlementsService {
    private final EntitlementsApiService entitlementsApiService;
    private final EntitlementsReadOnlyApiService entitlementsReadOnlyApiService;
    private final SubscriptionsShimApiService subscriptionsShimApiService;


    @Inject
    public DefaultEntitlementsService(EntitlementsApiService entitlementsApiService, EntitlementsReadOnlyApiService entitlementsReadOnlyApiService,
                                      SubscriptionsShimApiService subscriptionsShimApiService) {
        this.entitlementsApiService = entitlementsApiService;
        this.entitlementsReadOnlyApiService = entitlementsReadOnlyApiService;
        this.subscriptionsShimApiService = subscriptionsShimApiService;
    }

    @Override
    public Entitlement getEntitlement(UUID customerId, UUID entitlementId) {
        return entitlementsReadOnlyApiService.getEntitlement(customerId, entitlementId);
    }

    @Override
    public Entitlement[] getEntitlements(UUID customerId) {
        return subscriptionsShimApiService.getSubscriptionBasedEntitlements(customerId, "server", 250, 0);
    }
    @Override
    public Entitlement updateCommonName(UUID customerId, UUID entitlementId, String commonName) {
        EntitlementUpdateCommonNameRequest request = new EntitlementUpdateCommonNameRequest(commonName);
        return entitlementsApiService.updateCommonName(customerId, entitlementId, request);
    }

    @Override
    public Entitlement suspendEntitlement(UUID customerId, UUID entitlementId, String suspendReason) {
        Entitlement entitlement = entitlementsReadOnlyApiService.getEntitlement(customerId, entitlementId);
        int ifMatch = entitlement.metadata.revision;
        String idempotentId = UUID.randomUUID().toString();
        EntitlementSuspendReinstateRequest suspendReinstateRequest = new EntitlementSuspendReinstateRequest(suspendReason);
        return entitlementsApiService.suspendEntitlement(customerId, entitlementId, ifMatch, idempotentId, suspendReinstateRequest);
    }

    @Override
    public Entitlement reinstateEntitlement(UUID customerId, UUID entitlementId, String suspendReason) {
        Entitlement entitlement = entitlementsReadOnlyApiService.getEntitlement(customerId, entitlementId);
        int ifMatch = entitlement.metadata.revision;
        String idempotentId = UUID.randomUUID().toString();
        EntitlementSuspendReinstateRequest suspendReinstateRequest = new EntitlementSuspendReinstateRequest(suspendReason);
        return entitlementsApiService.reinstateEntitlement(customerId, entitlementId, ifMatch, idempotentId, suspendReinstateRequest);
    }

    @Override
    public Entitlement provisionEntitlement(UUID customerId, UUID entitlementId, String managementConsoleUrl, String commonName,
                                     String provisioningTracker, String result, String serviceStartDate, String serviceEndDate,
                                     String firstAutomatedDate, String lastPossibleDate) {
        ManagementConsole console = new ManagementConsole(managementConsoleUrl);
        EntitlementProvisionRequest.Service service = new EntitlementProvisionRequest.Service(serviceStartDate, serviceEndDate);
        EntitlementProvisionRequest.Renewal renewal = new EntitlementProvisionRequest.Renewal(firstAutomatedDate, lastPossibleDate);
        EntitlementProvisionRequest request = new EntitlementProvisionRequest(console, commonName, provisioningTracker, result, service, renewal);
        return entitlementsApiService.provisionEntitlement(customerId, entitlementId, request);
    }

    @Override
    public Entitlement updateEntitlementConsole(UUID customerId, UUID entitlementId, String managementConsoleUrl) {
        EntitlementManagementConsoleRequest request = new EntitlementManagementConsoleRequest(new ManagementConsole(managementConsoleUrl));
        return entitlementsApiService.updateConsole(customerId, entitlementId, request);
    }
}
