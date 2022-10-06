package com.godaddy.vps4.customer;

import java.util.UUID;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

@Path("/v1/customers")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public interface CustomerApiService {
    @GET
    @Path("/{customerId}")
    Customer getCustomer(@HeaderParam("authorization") String auth,
                         @PathParam("customerId") UUID customerId);
}
