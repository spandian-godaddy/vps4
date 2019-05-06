package com.godaddy.vps4.web.vm;

import java.util.UUID;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import com.godaddy.vps4.vm.VmAction;
import com.godaddy.vps4.web.Vps4Api;
import com.google.inject.Inject;

import io.swagger.annotations.Api;

@Vps4Api
@Api(tags = { "vms" })
@Path("/api/vms")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class VmRescueResource {

    @Inject
    public VmRescueResource() {
    }

    @POST
    @Path("{vmId}/rescue")
    public VmAction rescue(@PathParam("vmId") UUID vmId) {
        return new VmAction();
    }

    @POST
    @Path("{vmId}/endRescue")
    public VmAction endRescue(@PathParam("vmId") UUID vmId) {
        return new VmAction();
    }

    @GET
    @Path("{vmId}/rescueCredentials")
    public RescueCredentials getRescueCredentials(@PathParam("vmId") UUID vmId) {
        return new RescueCredentials("testUsername", "testPassword");
    }
}
