package com.godaddy.hfs.mailrelay;

import java.util.List;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import io.swagger.annotations.Api;

@Path("/api/v1/mailrelay")
@Api(tags = { "mailrelay" })
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public interface MailRelayService {

    @GET
    @Path("/{ipv4Address}")
    MailRelay getMailRelay(@PathParam("ipv4Address") String ipv4Address);

    @POST
    @Path("/{ipv4Address}")
    MailRelay setRelayQuota(
            @PathParam("ipv4Address") String ipv4Address, MailRelayUpdate request);

    @GET
    @Path("/{ipv4Address}/history")
    List<MailRelayHistory> getRelayHistory(
    		@PathParam("ipv4Address") String ipv4Address);

}
