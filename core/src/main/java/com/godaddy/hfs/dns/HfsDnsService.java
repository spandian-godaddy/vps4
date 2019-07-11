package com.godaddy.hfs.dns;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;

import io.swagger.annotations.Api;

@Path("/api/v1/dns")
@Api(tags={"dns"})

@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public interface HfsDnsService {

    @GET
    @Path("/actions/{actionId}/")
    HfsDnsAction getDnsAction(@PathParam("actionId") long dnsActionId);

    @POST
    @Path("/servers/{serverId}/reverse/")
    HfsDnsAction createDnsPtrRecord(@PathParam("serverId") long hfsVmId, @QueryParam("name") String reverseDnsName);

    @GET
    @Path("/servers/{serverId}/reverse/")
    RdnsRecords getReverseDnsName(@PathParam("serverId") long hfsVmId);
}
