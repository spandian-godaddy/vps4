package com.godaddy.vps4.entitlement;

import com.godaddy.vps4.entitlement.models.*;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.util.UUID;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class DefaultEntitlementsServiceTest {
    @Mock private EntitlementsApiService entitlementsApiService;
    @Mock private EntitlementsReadOnlyApiService entitlementsReadOnlyApiService;
    @Mock private SubscriptionsShimApiService subscriptionsShimApiService;

    @Captor private ArgumentCaptor<EntitlementUpdateCommonNameRequest> entitlementUpdateCommonNameReq;
    @Captor private ArgumentCaptor<EntitlementSuspendReinstateRequest> entitlementSuspendReinstateReq;
    @Captor private ArgumentCaptor<EntitlementManagementConsoleRequest> entitlementManagementConsoleReq;
    @Captor private ArgumentCaptor<EntitlementProvisionRequest> entitlementProvisionReq;
    private DefaultEntitlementsService service;

    private UUID customerId = UUID.randomUUID();

    private UUID orionGuid = UUID.randomUUID();
    private Entitlement entitlement;
    private Entitlement[] entitlements;
    @Before
    public void setUp() {
        EntitlementMetadata entitlementMetadata = new EntitlementMetadata();
        entitlementMetadata.revision = 2;
        entitlementMetadata.version = "fakeEntitlementVersion";
        entitlement = new Entitlement();
        entitlement.entitlementId = orionGuid;
        entitlement.customerId = customerId;
        entitlement.metadata = entitlementMetadata;
        entitlements = new Entitlement[]{entitlement};
        service = new DefaultEntitlementsService(entitlementsApiService, entitlementsReadOnlyApiService, subscriptionsShimApiService);
        when(entitlementsReadOnlyApiService.getEntitlement(customerId, orionGuid)).thenReturn(entitlement);
        when(entitlementsApiService.updateCommonName(any(), any(), any())).thenReturn(entitlement);
        when(entitlementsApiService.suspendEntitlement(any(), any(), eq(2), any(), any())).thenReturn(entitlement);
        when(entitlementsApiService.reinstateEntitlement(any(), any(), eq(2), any(), any())).thenReturn(entitlement);
        when(entitlementsApiService.updateConsole(any(), any(), any())).thenReturn(entitlement);
        when(entitlementsApiService.provisionEntitlement(any(), any(), any())).thenReturn(entitlement);
        when(subscriptionsShimApiService.getSubscriptionBasedEntitlements(customerId,"server", 250, 0)).thenReturn(entitlements);
    }

    @Test
    public void getEntitlementCallsEntitlementsReadOnlyApi() {
        Entitlement returnedEntitlement = service.getEntitlement(customerId, orionGuid);
        verify(entitlementsReadOnlyApiService).getEntitlement(customerId, orionGuid);
        assertSame(entitlement, returnedEntitlement);
    }

    @Test
    public void getEntitlementsCallsSubscriptionShimReadOnlyApi() {
        Entitlement[] returnedEntitlements = service.getEntitlements(customerId);
        assertSame(entitlements, returnedEntitlements);
        verify(subscriptionsShimApiService).getSubscriptionBasedEntitlements(customerId, "server", 250, 0);
    }

    @Test
    public void updateEntitlementCommonNameCallsEntitlementsApi() {
        Entitlement returnedEntitlement = service.updateCommonName(customerId, orionGuid, "newCommonName");
        assertSame(entitlement, returnedEntitlement);

        verify(entitlementsApiService).updateCommonName(eq(customerId), eq(orionGuid), entitlementUpdateCommonNameReq.capture());
        EntitlementUpdateCommonNameRequest req = entitlementUpdateCommonNameReq.getValue();
        assertEquals("newCommonName", req.commonName);
    }

    @Test
    public void suspendEntitlementCallsEntitlementsApi() {
        Entitlement returnedEntitlement = service.suspendEntitlement(customerId, orionGuid, "suspendReason");
        assertSame(entitlement, returnedEntitlement);

        verify(entitlementsApiService).suspendEntitlement(eq(customerId), eq(orionGuid), eq(2),
                any(String.class), entitlementSuspendReinstateReq.capture());

        EntitlementSuspendReinstateRequest req = entitlementSuspendReinstateReq.getValue();
        assertEquals("suspendReason", req.suspendReason);

    }

    @Test
    public void reinstateEntitlementCallsEntitlementsApi() {
        Entitlement returnedEntitlement = service.reinstateEntitlement(customerId, orionGuid, "suspendReason");
        assertSame(entitlement, returnedEntitlement);


        verify(entitlementsApiService).reinstateEntitlement(eq(customerId), eq(orionGuid), eq(2),
                any(String.class), entitlementSuspendReinstateReq.capture());

        EntitlementSuspendReinstateRequest req = entitlementSuspendReinstateReq.getValue();
        assertEquals("suspendReason", req.suspendReason);
    }

    @Test
    public void updateEntitlementConsoleCallsEntitlementsApi() {
        Entitlement returnedEntitlement = service.updateEntitlementConsole(customerId, orionGuid, "fakeUrl");
        assertSame(entitlement, returnedEntitlement);

        verify(entitlementsApiService).updateConsole(eq(customerId), eq(orionGuid), entitlementManagementConsoleReq.capture());

        EntitlementManagementConsoleRequest req = entitlementManagementConsoleReq.getValue();
        assertEquals("fakeUrl", req.managementConsole.url);
    }

    @Test
    public void provisionEntitlementCallsEntitlementsApi() {
        Entitlement returnedEntitlement = service.provisionEntitlement(customerId, orionGuid, "fakeUrl", "commonName",
                "provisioningTracker", "result",  "startTime",  "endTime",
                "firstAutomated", "lastPossible");
        assertSame(entitlement, returnedEntitlement);

        verify(entitlementsApiService).provisionEntitlement(eq(customerId), eq(orionGuid), entitlementProvisionReq.capture());

        EntitlementProvisionRequest req = entitlementProvisionReq.getValue();
        assertEquals("fakeUrl", req.managementConsole.url);
        assertEquals("commonName", req.commonName);
        assertEquals("provisioningTracker", req.provisioningTracker);
        assertEquals("result", req.result);
        assertEquals("startTime", req.service.start);
        assertEquals("endTime", req.service.end);
        assertEquals("firstAutomated", req.renewal.firstAutomated);
        assertEquals("lastPossible", req.renewal.lastPossible);
    }
}

