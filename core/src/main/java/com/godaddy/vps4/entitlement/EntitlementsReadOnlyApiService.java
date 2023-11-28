package com.godaddy.vps4.entitlement;

import com.godaddy.vps4.entitlement.models.Entitlement;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import java.util.UUID;

@Path("/v2")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public interface EntitlementsReadOnlyApiService {
    @GET
    @Path("/customers/{customerId}/entitlements/{entitlementId}")
    Entitlement getEntitlement(@PathParam("customerId") UUID customerId, @PathParam("entitlementId") UUID entitlementId);
}
