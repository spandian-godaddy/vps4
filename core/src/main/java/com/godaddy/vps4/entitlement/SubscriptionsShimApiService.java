package com.godaddy.vps4.entitlement;

import com.godaddy.vps4.entitlement.models.Entitlement;


import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Consumes;
import javax.ws.rs.Produces;
import javax.ws.rs.PathParam;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import java.util.UUID;

@Path("/v2")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public interface SubscriptionsShimApiService {
    @GET
    @Path("/customers/{customerId}/entitlements")
    Entitlement[] getSubscriptionBasedEntitlements(@PathParam("customerId") UUID customerId, @QueryParam("productFamilies") String productFamilies,
                                                   @QueryParam("limit") int limit, @QueryParam("offset") int offset);
}
