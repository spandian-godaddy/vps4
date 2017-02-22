package com.godaddy.vps4.web.smtp;

import java.util.UUID;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import com.godaddy.vps4.web.Vps4Api;

import io.swagger.annotations.Api;

@Vps4Api
@Api(tags = { "vms" })

@Path("/api/vms")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class VmSmtpResource {

    @GET
    @Path("{vmId}/smtp/current")
    public int getCurrentSmtpUsage(@PathParam("vmId") UUID vmId) {
        return 0;
    }
    
    @GET
    @Path("{vmId}/smtp/history")
    public void getSmtpUsageHistory(@PathParam("vmId") UUID vmId) {
        
    }
    
}
