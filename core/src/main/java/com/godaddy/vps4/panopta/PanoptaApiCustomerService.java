package com.godaddy.vps4.panopta;

import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;

@Path("/v2/customer")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public interface PanoptaApiCustomerService {

    @POST
    @Path("/")
    void createCustomer(PanoptaApiCustomerRequest panoptaApiCustomerRequest);

    @GET
    @Path("/")
    PanoptaApiCustomerList getCustomer(@QueryParam("partner_key") String partnerCustomerKey);

    @GET
    @Path("/")
    PanoptaApiCustomerList getCustomersByStatus(@QueryParam("partner_key") String partnerCustomerKey,
                                                @DefaultValue("active") @QueryParam("status") String status);

    @DELETE
    @Path("/{customer_key}")
    void deleteCustomer(@PathParam("customer_key") String customerKey);
}
