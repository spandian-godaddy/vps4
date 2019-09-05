package com.godaddy.vps4.panopta;


import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;

@Path("/v2/server")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public interface PanoptaApiServerService {
    @GET
    @Path("/{server_id}/agent_resource/{agent_resource_id}/metric/{timescale}")
    PanoptaServerMetric getMetricData(@PathParam("server_id") int serverId,
                                      @PathParam("agent_resource_id") int agentResourceId,
                                      @PathParam("timescale") String timescale,
                                      @QueryParam("partner_customer_key") String partnerCustomerKey);


    @GET
    @Path("/{server_id}/agent_resource")
    PanoptaAgentResourceList getAgentResourceList(@PathParam("server_id") int serverId,
                                      @QueryParam("partner_customer_key") String partnerCustomerKey);

    @GET
    @Path("/")
    PanoptaServers getPanoptaServers(@QueryParam("partner_customer_key") String partnerCustomerKey);

    @GET
    @Path("/{server_id}")
    PanoptaServers.Server getServer(@PathParam("server_id") int serverId,
                            @QueryParam("partner_customer_key") String partnerCustomerKey);

    @PUT
    @Path("/{server_id}")
    void setServerStatus(@PathParam("server_id") int serverId,
                         @QueryParam("partner_customer_key") String partnerCustomerKey,
                         PanoptaApiUpdateServerRequest panoptaApiUpdateServerRequest);

    @GET
    @Path("/{server_id}/availability")
    PanoptaAvailability getAvailability(@PathParam("server_id") int serverId,
                                        @QueryParam("partner_customer_key") String partnerCustomerKey,
                                        @QueryParam("start_time") String startTime,
                                        @QueryParam("end_time") String endTime);

    @DELETE
    @Path("/{server_id}")
    void deleteServer(@PathParam("server_id") int serverId,
            @QueryParam("partner_customer_key") String partnerCustomerKey);
}
