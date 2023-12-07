package com.godaddy.vps4.panopta;


import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
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
    @POST
    @Path("/")
    void createServer(@QueryParam("partner_customer_key") String partnerCustomerKey,
                      PanoptaApiServerRequest panoptaApiServerRequest);
    @POST
    @Path("/{server_id}/template")
    void applyTemplate(@PathParam("server_id") long serverId,
                       @QueryParam("partner_customer_key") String partnerCustomerKey,
                       PanoptaApiApplyTemplateRequest panoptaApiApplyTemplateRequest);
    @DELETE
    @Path("/{server_id}/template/{template_id}")
    void removeTemplate(@PathParam("server_id") long serverId,
                        @QueryParam("partner_customer_key") String partnerCustomerKey,
                        @PathParam("template_id") String templateId, PanoptaApiRemoveTemplateRequest request);

    @GET
    @Path("/{server_id}/agent_resource")
    PanoptaUsageIdList getUsageList(@PathParam("server_id") long serverId,
                                    @QueryParam("partner_customer_key") String partnerCustomerKey,
                                    @QueryParam("limit") int limit);

    @GET
    @Path("/{server_id}/network_service")
    PanoptaNetworkIdList getNetworkList(@PathParam("server_id") long serverId,
                                        @QueryParam("partner_customer_key") String partnerCustomerKey,
                                        @QueryParam("limit") int limit);

    @GET
    @Path("/{server_id}/agent_resource/{agent_resource_id}/metric/{timescale}")
    PanoptaUsageGraph getUsageGraph(@PathParam("server_id") long serverId,
                                    @PathParam("agent_resource_id") long agentResourceId,
                                    @PathParam("timescale") String timescale,
                                    @QueryParam("partner_customer_key") String partnerCustomerKey);

    @GET
    @Path("/{server_id}/network_service/{network_service_id}/response_time/{timescale}")
    PanoptaNetworkGraph getNetworkGraph(@PathParam("server_id") long serverId,
                                        @PathParam("network_service_id") long networkServiceId,
                                        @PathParam("timescale") String timescale,
                                        @QueryParam("partner_customer_key") String partnerCustomerKey);

    @GET
    @Path("/{server_id}")
    PanoptaServers.Server getServer(@PathParam("server_id") long serverId,
                                    @QueryParam("partner_customer_key") String partnerCustomerKey);

    @GET
    @Path("/")
    PanoptaServers getServers(@QueryParam("partner_customer_key") String partnerCustomerKey,
                              @QueryParam("fqdn") String fqdn,
                              @QueryParam("name") String name,
                              @QueryParam("status") String status);

    @PUT
    @Path("/{server_id}")
    void updateServer(@PathParam("server_id") long serverId,
                      @QueryParam("partner_customer_key") String partnerCustomerKey,
                      PanoptaApiUpdateServerRequest panoptaApiUpdateServerRequest);

    @DELETE
    @Path("/{server_id}")
    void deleteServer(@PathParam("server_id") long serverId,
                      @QueryParam("partner_customer_key") String partnerCustomerKey);

    @POST
    @Path("/{server_id}/network_service")
    void addNetworkService(@PathParam("server_id") long serverId,
                           @QueryParam("partner_customer_key") String partnerCustomerKey,
                           PanoptaApiNetworkServiceRequest request);

    @DELETE
    @Path("{server_id}/network_service/{network_service_id}")
    void deleteNetworkService(@PathParam("server_id") long serverId,
                              @PathParam("network_service_id") long networkServiceId,
                              @QueryParam("partner_customer_key") String partnerCustomerKey);

    @POST
    @Path("/{server_id}/server_attribute")
    void setAttribute(@PathParam("server_id") long serverId,
                      @QueryParam("partner_customer_key") String partnerCustomerKey,
                      PanoptaApiAttributeRequest request);

    @GET
    @Path("/{server_id}/availability")
    PanoptaAvailability getAvailability(@PathParam("server_id") long serverId,
                                        @QueryParam("partner_customer_key") String partnerCustomerKey,
                                        @QueryParam("start_time") String startTime,
                                        @QueryParam("end_time") String endTime);

    @GET
    @Path("/{server_id}/outage")
    PanoptaOutageList getOutages(@PathParam("server_id") long serverId,
                                 @QueryParam("partner_customer_key") String partnerCustomerKey,
                                 @QueryParam("status") String status,
                                 @QueryParam("limit") int limit,
                                 @QueryParam("start_time") String startTime);
}
