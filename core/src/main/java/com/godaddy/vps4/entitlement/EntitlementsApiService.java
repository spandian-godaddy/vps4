package com.godaddy.vps4.entitlement;

import com.godaddy.vps4.entitlement.models.EntitlementUpdateCommonNameRequest;
import com.godaddy.vps4.entitlement.models.EntitlementCancelRequest;
import com.godaddy.vps4.entitlement.models.EntitlementSuspendReinstateRequest;
import com.godaddy.vps4.entitlement.models.EntitlementManagementConsoleRequest;
import com.godaddy.vps4.entitlement.models.EntitlementProvisionRequest;
import com.godaddy.vps4.entitlement.models.Entitlement;

import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Consumes;
import javax.ws.rs.Produces;
import javax.ws.rs.PathParam;
import javax.ws.rs.HeaderParam;

import javax.ws.rs.core.MediaType;
import java.util.UUID;

@Path("/v2")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public interface EntitlementsApiService {
    @POST
    @Path("/customers/{customerId}/entitlements/{entitlementId}/reinstate")
    Entitlement reinstateEntitlement(@PathParam("customerId") UUID customerId, @PathParam("entitlementId") UUID entitlementId,
                              @HeaderParam("If-Match") int ifMatch, @HeaderParam("Idempotent-Id") String idempotentId,
                              EntitlementSuspendReinstateRequest suspendReinstateRequest);

    @POST
    @Path("/customers/{customerId}/entitlements/{entitlementId}/suspend")
    Entitlement suspendEntitlement(@PathParam("customerId") UUID customerId, @PathParam("entitlementId") UUID entitlementId,
                            @HeaderParam("If-Match") int ifMatch, @HeaderParam("Idempotent-Id") String idempotentId,
                            EntitlementSuspendReinstateRequest suspendReinstateRequest);

    @POST
    @Path("/customers/{customerId}/entitlements/{entitlementId}/commonName")
    Entitlement updateCommonName(@PathParam("customerId") UUID customerId, @PathParam("entitlementId") UUID entitlementId,
                          EntitlementUpdateCommonNameRequest updateCommonNameRequest);

    @POST
    @Path("/customers/{customerId}/entitlements/{entitlementId}/managementConsole")
    Entitlement updateConsole(@PathParam("customerId") UUID customerId, @PathParam("entitlementId") UUID entitlementId,
                       EntitlementManagementConsoleRequest managementConsoleRequest);

    @POST
    @Path("/customers/{customerId}/entitlements/{entitlementId}/provision")
    Entitlement provisionEntitlement(@PathParam("customerId") UUID customerId, @PathParam("entitlementId") UUID entitlementId,
                              EntitlementProvisionRequest entitlementProvisionRequest);
}
