package com.godaddy.vps4.shopper;

import com.godaddy.vps4.shopper.model.Shopper;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;

@Path("/v1")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public interface ShopperApiService {
    @GET
    @Path("/customers/{customerId}/shopper")
    Shopper getShopperByCustomerId(@PathParam("customerId") String customerId, @QueryParam("auditClientIp") String auditClientIp);

    @GET
    @Path("/shoppers/{shopperId}")
    Shopper getShopper(@PathParam("shopperId") String shopperId, @QueryParam("auditClientIp") String auditClientIp);
}