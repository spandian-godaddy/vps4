package com.godaddy.vps4.panopta;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;

@Path("/v2/outage")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public interface PanoptaApiOutageService {
    @GET
    @Path("/{outage_id}")
    PanoptaOutage getOutage(@PathParam("outage_id") long outageId,
                            @QueryParam("partner_customer_key") String partnerCustomerKey);
}
